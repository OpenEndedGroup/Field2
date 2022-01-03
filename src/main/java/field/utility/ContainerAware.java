package field.utility;

/**
 * used during duplication of attribtues for values that want to know what object they are in
 */
public interface ContainerAware<T> {

	Object moveContainer(T to);
}
