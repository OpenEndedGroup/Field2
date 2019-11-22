
var videoLayer = _.stage.withImageSequence("j:/MASTER_DANCE_PRORES_HQ_HD_FELDMANN_both_jp_3D.mov.dir")


z = -2
var f = new FLine()
f.moveTo(0,0,z)
f.node().texCoord = vec(0.0,0.0)
f.lineTo(2,0,z)
f.node().texCoord = vec(1,0.)
f.lineTo(2,1,z)
f.node().texCoord = vec(1,1)
f.lineTo(0,1,z)
f.node().texCoord = vec(0, 1)
f.lineTo(0,0,z)
f.node().texCoord = vec(0, 0)

f.filled=true
f.stroked=false
f.color=vec(1,1,1,1)

videoLayer.lines.f = f + vec(-1,-1,0)

videoLayer.bindTriangleShader(_)



while(_.wait())
{
	videoLayer.time = _t()
}
