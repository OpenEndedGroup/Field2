package field.graphics.vr;

import field.graphics.Camera;
import field.graphics.FBO;
import field.graphics.Scene;
import field.graphics.StereoCameraInterface;
import field.linalg.Mat4;
import field.linalg.Quat;
import field.linalg.Vec3;
import field.utility.Rect;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.ovr.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;
import static org.lwjgl.ovr.OVR.*;
import static org.lwjgl.ovr.OVRErrorCode.ovrSuccess;
import static org.lwjgl.ovr.OVRErrorCode.ovrSuccess_NotVisible;
import static org.lwjgl.ovr.OVRKeys.OVR_KEY_EYE_HEIGHT;
import static org.lwjgl.ovr.OVRUtil.ovr_Detect;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

/**
 * Draw target (like a window) for Oculus
 */
public class OculusDrawTarget2 {

    private long hmd;

    private final OVRFovPort fovPorts[] = new OVRFovPort[2];
    private final OVRMatrix4f[] projections = new OVRMatrix4f[2];
    private final OVREyeRenderDesc eyeRenderDesc[] = new OVREyeRenderDesc[2];

    public Mat4 additionalView = new Mat4().identity();


    public final OVRPosef eyePoses[] = new OVRPosef[2];
    public Vec3 playerEyePos;
    public int textureW;
    public int textureH;
    private int texturesPerEyeCount = 1;
    public OVRLayerEyeFov layer0;
    public FBO[] fbuffers;
    private PointerBuffer layers;
    private PointerBuffer textureSetPB;

    public Vec3 extraCameraTranslation = new Vec3();

    int currentTPEIndex;
    private OVRRecti[] viewport;
    OVRLogCallback callback = new OVRLogCallback() {
        @Override
        public void invoke(long userdata, int level, long message) {
            System.out.println("" +
                    "LibOVR [" + level + "] ");
            System.out.println("message is :" + memASCII(message));
        }
    };
    private int explicitTextureSet;

    public boolean pauseCamera = false;

    public Rect renderViewport;
    public float renderEye;


    private Mat4 thisModelView;
    private Mat4 thisProjection;

    private Mat4[] theseModelViews = new Mat4[2];
    private Mat4[] theseProjections = new Mat4[2];

    private int chainLengh;
    private FBO fpreview;
    public Quat orientation;
    public Vec3 translation;

    public Mat4 view() {
        if (thisModelView == null)
            System.err.println(" warning : view called while we were not rendering to an oculus eye");
        return thisModelView == null ? new Mat4() : thisModelView;
    }

    public Mat4 projectionMatrix() {
        if (thisProjection == null)
            System.err.println(" warning : projectionMatrix called while we were not rendering to an oculus eye");
        return thisProjection == null ? new Mat4() : thisProjection;
    }

    public Mat4 leftView() {
        if (theseModelViews == null)
            System.err.println(" warning : view called while we were not rendering to an oculus eye");
        return theseModelViews == null ? new Mat4() : theseModelViews[0];
    }

    public Mat4 rightView() {
        if (theseModelViews == null)
            System.err.println(" warning : view called while we were not rendering to an oculus eye");
        return theseModelViews == null ? new Mat4() : theseModelViews[1];
    }

    public Mat4 leftProjectionMatrix() {
        if (theseProjections == null)
            System.err.println(" warning : projectionMatrix called while we were not rendering to an oculus eye");
        return theseProjections == null ? new Mat4() : theseProjections[0];
    }

    public Mat4 rightProjectionMatrix() {
        if (theseProjections == null)
            System.err.println(" warning : projectionMatrix called while we were not rendering to an oculus eye");
        return theseProjections == null ? new Mat4() : theseProjections[1];
    }


    public StereoCameraInterface cameraInterface()
    {
        return new StereoCameraInterface() {
            @Override
            public Mat4 projectionMatrix(float stereoSide) {
                if (stereoSide<0) return leftProjectionMatrixT();
                return rightProjectionMatrixT();
            }

            @Override
            public Mat4 view(float stereoSide) {
                Mat4 V;
                if (stereoSide<0) V = new Mat4(leftViewT());
                V = new Mat4(rightViewT());

                Mat4 r = new Mat4().rotate(Math.PI, new Vec3(1,0,0));

                V = new Mat4(r).mul(V);
                return V;
            }

            @Override
            public Vec3 getPosition() {
                return playerEyePos;
            }
        };
    }

    public Mat4 leftViewT() {
        if (theseModelViews == null)
            System.err.println(" warning : view called while we were not rendering to an oculus eye");
        return theseModelViews[0] == null ? new Mat4() : new Mat4(theseModelViews[0]).transpose();
    }

    public Mat4 rightViewT() {
        if (theseModelViews == null)
            System.err.println(" warning : view called while we were not rendering to an oculus eye");
        return theseModelViews[1] == null ? new Mat4() : new Mat4(theseModelViews[1]).transpose();
    }

    public Mat4 leftProjectionMatrixT() {
        if (theseProjections == null)
            System.err.println(" warning : projectionMatrix called while we were not rendering to an oculus eye");
        return theseProjections[0] == null ? new Mat4() : new Mat4(theseProjections[0]).transpose();
    }

    public Mat4 rightProjectionMatrixT() {
        if (theseProjections == null)
            System.err.println(" warning : projectionMatrix called while we were not rendering to an oculus eye");
        return theseProjections[1] == null ? new Mat4() : new Mat4(theseProjections[1]).transpose();
    }


    int warmUp = 0;

    boolean go = false;

    public FBO getPreviewTexture()
    {
        return fpreview;
    }

    public Scene getScene() {
        return fbuffers[0].scene;
    }

    // turn on to jam this onto the screen (SBS)
    public boolean debugBlit = false;

    // turn on to jam this into an FBO (SBS)
    public boolean debugFBO = false;


    public float aspectRatio = 1;
    public int baseW;
    public int baseH;


    public void resetViewNow() {

        OVR.ovr_RecenterTrackingOrigin(hmd);

    }


    public void init(Scene w) {

        w.attach("__initoculus__", new Scene.Perform() {

            @Override
            public boolean perform(int pass) {

                if (warmUp++ < 5) return true;

                OVRDetectResult detect = OVRDetectResult.calloc();
                ovr_Detect(0, detect);

                System.out.println("ovrHmd_Detect = " + detect.IsOculusHMDConnected() + " " + detect.IsOculusServiceRunning());

                OVRInitParams initParams = OVRInitParams.calloc();
                initParams.LogCallback(callback);
//        initParams.Flags(ovrInit_Debug);
                int init = ovr_Initialize(initParams);

                System.out.println("ovr_Initialize = " + init);
                System.out.println("ovr_GetVersionString = " + ovr_GetVersionString());


                PointerBuffer hmd_p = memAllocPointer(1);
                OVRGraphicsLuid luid = OVRGraphicsLuid.calloc();
                hmd = ovr_Create(hmd_p, luid); // TODO

                System.out.println(" create CV1 :" + hmd + " / " + hmd_p + " / " + hmd_p.get(0));

                hmd = hmd_p.get(0);

                OVRHmdDesc d = OVRHmdDesc.calloc();//new OVRHmdDesc(MemoryUtil.memByteBuffer(hmd, OVRHmdDesc.SIZEOF));
                ovr_GetHmdDesc(hmd, d);
//				syntheticToString(d);

                int resolutionW = d.Resolution().w();
                int resolutionH = d.Resolution().h();
                float canvasRatio = (float) resolutionW / resolutionH;

                System.out.println(" aspect ratio :" + canvasRatio + " @ " + resolutionW + " x " + resolutionH);

                aspectRatio = canvasRatio;
                baseW = resolutionW;
                baseH = resolutionH;

                for (int eye = 0; eye < 2; eye++) {
                    fovPorts[eye] = d.DefaultEyeFov(eye);
                    System.out.println("eye " + eye + " = " + fovPorts[eye].UpTan() + ", " + fovPorts[eye].DownTan() + ", " + fovPorts[eye].LeftTan() + ", " + fovPorts[eye].RightTan());
                }

                playerEyePos = new Vec3(0.0f, -ovr_GetFloat(hmd, OVR_KEY_EYE_HEIGHT, 1.65f), 0.0f);

                System.out.println("step 5 - projections");
                for (int eye = 0; eye < 2; eye++) {
                    projections[eye] = OVRMatrix4f.malloc();
//					OVRUtil.ovrMatrix4f_Projection(fovPorts[eye], 0.5f, 500f, OVRUtil.ovrProjection_RightHanded, projections[eye]);
                    OVRUtil.ovrMatrix4f_Projection(fovPorts[eye], 0.5f, 5000f, OVRUtil.ovrProjection_ClipRangeOpenGL, projections[eye]);
                }

                // step 6 - render desc
                System.out.println("step 6 - render desc");
                for (int eye = 0; eye < 2; eye++) {
                    eyeRenderDesc[eye] = OVREyeRenderDesc.malloc();
                    ovr_GetRenderDesc(hmd, eye, fovPorts[eye], eyeRenderDesc[eye]);
                    System.out.println("ipd eye " + eye + " = " + eyeRenderDesc[eye].HmdToEyeOffset().x());
                }

                // docs claim there's no reason for this to be above 1.0
                float pixelsPerDisplayPixel = 1.0f;

                OVRSizei leftTextureSize = OVRSizei.malloc();
                ovr_GetFovTextureSize(hmd, ovrEye_Left, fovPorts[ovrEye_Left], pixelsPerDisplayPixel, leftTextureSize);
                System.out.println("leftTextureSize W=" + leftTextureSize.w() + ", H=" + leftTextureSize.h());

                OVRSizei rightTextureSize = OVRSizei.malloc();
                ovr_GetFovTextureSize(hmd, ovrEye_Right, fovPorts[ovrEye_Right], pixelsPerDisplayPixel, rightTextureSize);
                System.out.println("rightTextureSize W=" + rightTextureSize.w() + ", H=" + rightTextureSize.h());


                textureW = (leftTextureSize.w() + rightTextureSize.w()) / 2;
                textureH = Math.max(leftTextureSize.h(), rightTextureSize.h());
                System.out.println("request textureW=" + textureW + ", textureH=" + textureH);
                leftTextureSize.free();
                rightTextureSize.free();
                System.err.println(" -- inside opengl --");

                textureSetPB = memAllocPointer(1);


                // BEEN ALL OVER THIS BIT

                OVRTextureSwapChainDesc swapChainDesc = OVRTextureSwapChainDesc.create();

                swapChainDesc.set(ovrTexture_2D, OVR_FORMAT_R8G8B8A8_UNORM_SRGB, 1, textureW * 2, textureH, 1, 1, false, 0, 0);

                if (OVRGL.ovr_CreateTextureSwapChainGL(hmd, swapChainDesc, textureSetPB) != ovrSuccess) {      // twice width for single texture
                    throw new IllegalStateException("Failed to create Swap Texture Set");
                }
                long hts = textureSetPB.get(0);

                int[] len = {0};
                OVR.ovr_GetTextureSwapChainLength(hmd, textureSetPB.get(0), len);
                System.err.println(" swap chain length is " + len[0]);

                chainLengh = len[0];

                IntBuffer texID = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
                System.err.println(" texture id is :" + texID.get(0));

                fbuffers = new FBO[chainLengh];
                for (int i = 0; i < chainLengh; i++) {
                    OVRGL.ovr_GetTextureSwapChainBufferGL(hmd, textureSetPB.get(0), i, texID);
                    fbuffers[i] = new FBO(FBO.FBOSpecification.rgba(texID.get(0), textureW * 2, textureH).setOverrideTextureID(texID.get(0)));
                }

                fpreview = new FBO(FBO.FBOSpecification.rgba(texID.get(0), textureW * 2, textureH));

                fpreview.scene.attach(0, new Scene.Perform() {
                    @Override
                    public boolean perform(int pass) {

                        if (currentTPEIndex!=0) return true;

                        glClearColor(0.2f, 0.4f, 0, 1);
                        glClear(GL_COLOR_BUFFER_BIT);
                        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbuffers[currentTPEIndex].getOpenGLFrameBufferNameInCurrentContext());
                        glBlitFramebuffer(0, 0, textureW * 2, textureH, 0, 0, textureW * 2, textureH, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                        return true;
                    }
                });

                for (int tex = 1; tex < chainLengh; tex++) {
                    fbuffers[tex].scene = fbuffers[tex - 1].scene;
                }


                // eye viewports
                viewport = new OVRRecti[2]; //should not matter which texture we measure, but they might be different to what was requested.
                viewport[0] = OVRRecti.calloc();
                viewport[0].Pos().x(0);
                viewport[0].Pos().y(0);
                viewport[0].Size().w(textureW);
                viewport[0].Size().h(textureH);
                viewport[1] = OVRRecti.calloc();
                viewport[1].Pos().x(textureW);
                viewport[1].Pos().y(0);
                viewport[1].Size().w(textureW);
                viewport[1].Size().h(textureH);

                final Camera c = new Camera();

                currentTPEIndex = -1;

                for (int tex = 0; tex < 1; tex++) {
                    final int finalEyeIndex = tex;
                    fbuffers[tex].scene.attach(-100, (Scene.Perform) x -> {

//						GL11.glClearColor(1, renderEye, 1 - renderEye, 1);

//						GL11.glClearColor(0, 0, 0, 1);
//						GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                        GL11.glEnable(GL_DEPTH_CLAMP);
                        GL11.glEnable(GL_BLEND);
                        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        GL11.glEnable(GL_MULTISAMPLE);
                        GL11.glEnable(GL_POLYGON_SMOOTH);
//						System.out.println(" current p :" + c.view()+" "+finalEyeIndex);

                        return true;
                    });
                }


                go = true;


                return false;
            }

            @Override
            public int[] getPasses() {
                return new int[]{-1};
            }
        });

        w.attach("__renderoculus__", new Scene.Perform() {

            FloatBuffer fb = BufferUtils.createFloatBuffer(16);
            FloatBuffer fbP = BufferUtils.createFloatBuffer(16);


            @Override
            public boolean perform(int pass) {

                if (!go) return true;

                OVRSessionStatus stat = OVRSessionStatus.create();
                OVR.ovr_GetSessionStatus(hmd, stat);
                System.out.println(" status :"+stat.DisplayLost()+" "+stat.HmdMounted()+" "+stat.HmdPresent()+" "+stat.ShouldQuit()+" "+stat.ShouldRecenter());
                System.out.println(" status -- :"+stat.IsVisible());

//				System.exit(0);

                double ftiming = ovr_GetPredictedDisplayTime(hmd, 0);
                OVRTrackingState hmdState = OVRTrackingState.malloc();
                ovr_GetTrackingState(hmd, ftiming, true, hmdState);

                //get head pose
                OVRPosef headPose = hmdState.HeadPose().ThePose();
                hmdState.free();

                //build view offsets struct
                OVRVector3f.Buffer HmdToEyeOffsets = OVRVector3f.calloc(2);
                HmdToEyeOffsets.put(0, eyeRenderDesc[ovrEye_Left].HmdToEyeOffset());
                HmdToEyeOffsets.put(1, eyeRenderDesc[ovrEye_Right].HmdToEyeOffset());

                //calculate eye poses
                OVRPosef.Buffer outEyePoses = OVRPosef.create(2);
                OVRUtil.ovr_CalcEyePoses(headPose, HmdToEyeOffsets, outEyePoses);
                eyePoses[ovrEye_Left] = outEyePoses.get(0);
                eyePoses[ovrEye_Right] = outEyePoses.get(1);

                currentTPEIndex += 1;
                currentTPEIndex %= chainLengh;
                int[] ce = {0};
                OVR.ovr_GetTextureSwapChainCurrentIndex(hmd, textureSetPB.get(0), ce);
                currentTPEIndex = ce[0];

//				textureSetOne.CurrentIndex(currentTPEIndex);

                double sensorSampleTime = ovr_GetTimeInSeconds();

                IntBuffer texID = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
                OVRGL.ovr_GetTextureSwapChainBufferGL(hmd, textureSetPB.get(0), currentTPEIndex, texID);
//				System.err.println(" texture id is now :"+texID.get(0)+" for index "+currentTPEIndex);

                //Layers
                layer0 = OVRLayerEyeFov.calloc();
                layer0.Header().Type(ovrLayerType_EyeFov);
                layer0.Header().Flags(ovrLayerFlag_TextureOriginAtBottomLeft | ovrLayerFlag_HighQuality);



                int types = OVR.ovr_GetConnectedControllerTypes(hmd);
                System.out.println("\n controller types :"+types);


                OVRInputState inputState = OVRInputState.create();
                OVR.ovr_GetInputState(hmd, OVR.ovrControllerType_Touch, inputState);


                System.out.println("::"+OVRInputState.INDEXTRIGGER);

                System.out.println("== "+inputState.Buttons()+" "+inputState.Touches());
                System.out.println("== "+inputState.IndexTrigger(0)+" "+inputState.IndexTrigger(1));
                System.out.println("== "+inputState.HandTrigger(0)+" "+inputState.HandTrigger(1));
                System.out.println("== "+inputState.Thumbstick(0).x()+"/"+inputState.Thumbstick(0).y()+"   "+inputState.Thumbstick(1).x()+"/"+inputState.Thumbstick(1).y());

				/*
                OVRPoseStatef handPose = hmdState.HandPoses(0);
				OVRVector3f po = handPose.ThePose().Position();
				OVRQuatf qo = handPose.ThePose().Orientation();
				System.out.println(" handPose :"+po.x()+" "+po.y()+" "+po.z());
				System.out.println(" handPose :"+qo.x()+" "+qo.y()+" "+qo.z()+" "+qo.w());
*/


                for (int eyeIndex = 0; eyeIndex < 2; eyeIndex++) {
                    int eye = eyeIndex;


                    //eye = 1;

                    OVRPosef eyePose = eyePoses[eye];
                    layer0.RenderPose(eye, eyePose);

                    //setup matrix for eyes and the like based on eyePose then draw it.
                    renderViewport = new Rect(eyeIndex * textureW, 0, textureW, textureH);
                    renderEye = eyeIndex;

                    Mat4 matP = new Mat4();

                    fbP.rewind();

                    Mat4 proj = new Mat4(projections[eye].M()).transpose();
                    matP.set(proj).get(fbP);

                    Mat4 mat = new Mat4();

                    mat.identity();

                    Vec3 offsetPosition = new Vec3(eyeRenderDesc[eye].HmdToEyeOffset().x(), eyeRenderDesc[eye].HmdToEyeOffset().y(), eyeRenderDesc[eye].HmdToEyeOffset().z());

                    if (!pauseCamera) {
                        orientation = new Quat(eyePose.Orientation().x(), eyePose.Orientation().y(), eyePose.Orientation().z(), eyePose.Orientation().w());
                        orientation.invert();
                    }
                    mat.rotate(orientation);

                    Vec3 position = new Vec3(-eyePose.Position().x(), -eyePose.Position().y(), -eyePose.Position().z()).add(extraCameraTranslation);
                    mat.translate(position);
                    mat.translate(playerEyePos);    //back to 'floor' height


//					System.out.println(" translation :"+position+" "+playerEyePos+" "+mat.getTranslation(new Vec3()));
//					translation = mat.getTranslation(new Vec3());
                    if (!pauseCamera)
                        translation = new Vec3(position);


                    if (!pauseCamera) {
                        thisProjection = new Mat4(matP);

                        Mat4 at = new Mat4(additionalView);

                        thisModelView = theseModelViews[eye] = new Mat4(mat).mul(at);

                        theseProjections[eye] = new Mat4(matP);

                    }



//					fbuffers[currentTPEIndex].setViewport(renderViewport);

                    renderEye = eye;
//					System.out.println(" go to draw on FBO, eye "+eyeIndex);

                    // draw both eyes together with clipplane / instancing trick
                    glEnable(GL_CLIP_PLANE0);
                    if (eye == 1)
                        try {
                            fbuffers[currentTPEIndex].draw();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            System.exit(0);
                        }


                }

                OVR.ovr_CommitTextureSwapChain(hmd, textureSetPB.get(0));
                thisModelView = null;
                thisProjection = null;

                OVRViewScaleDesc scale = OVRViewScaleDesc.calloc();
                scale.HmdSpaceToWorldScaleInMeters(1);
                scale.HmdToEyeOffset(0, eyeRenderDesc[ovrEye_Left].HmdToEyeOffset());
                scale.HmdToEyeOffset(1, eyeRenderDesc[ovrEye_Right].HmdToEyeOffset());


                for (int eye = 0; eye < 2; eye++) {
                    layer0.ColorTexture(eye, textureSetPB.get(0));
                    layer0.Viewport(eye, viewport[eye]);
                    layer0.Fov(eye, fovPorts[eye]);
                    layer0.RenderPose(eye, eyePoses[eye]);
                }


                layer0.SensorSampleTime(sensorSampleTime);

                layers = memAllocPointer(1);
                layers.put(0, layer0);


                if (debugBlit) {
                    glClearColor(0.2f, 0, 0, 1);
                    glClear(GL_COLOR_BUFFER_BIT);
                    glBindFramebuffer(GL_READ_FRAMEBUFFER, fbuffers[currentTPEIndex].getOpenGLFrameBufferNameInCurrentContext());
                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                    glBlitFramebuffer(0, 0, textureW * 2, textureH, 0, 0, textureW * 2, textureH, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                }
                if (debugFBO)
                {
                    fpreview.draw();
                }

//				System.out.println(" size is :" + textureW + " " + textureH);

//				System.out.println("\n================= SUBMIT\n");
                int result = ovr_SubmitFrame(hmd, 0, scale, layers);
//				System.out.println("\n================= SUBMITED\n");
                if (result == ovrSuccess_NotVisible) {
                    notVisible = true;
//					System.out.println("TODO not vis!!");
                } else if (result != ovrSuccess) {
                    System.out.println("TODO failed submit");
                }
                else
                    notVisible = false;



                glfwSwapInterval(0);

                return true;
            }

            @Override
            public int[] getPasses() {
                return new int[]{0};
            }
        });


//		RunLoop.main.enterMainLoop();

    }

    boolean notVisible = true;

    public boolean isVisible()
    {
        return !notVisible;
    }

    private String string4f(ByteBuffer a) {
        FloatBuffer f = a.asFloatBuffer();
        return f.get() + " " + f.get() + " " + f.get() + " " + f.get();
    }


    private String string4i(ByteBuffer a) {
        IntBuffer f = a.asIntBuffer();
        return f.get() + " " + f.get() + " " + f.get() + " " + f.get();
    }

    public ByteBuffer bufferFromFloat(float f) {
        ByteBuffer q = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        q.asFloatBuffer().put(f);
        return q;
    }

    public ByteBuffer bufferFrom4Float(float a, float b, float c, float d) {
        ByteBuffer q = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        q.asFloatBuffer().put(a).put(b).put(c).put(d);
        return q;
    }

    private void syntheticToString(Object d) {
        Method[] m = d.getClass().getMethods();
        for (Method mm : m) {
            if (mm.getName().startsWith("get") && mm.getParameterCount() == 0) {
                try {
                    System.out.println(mm.getName() + " -> " + mm.invoke(d));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
