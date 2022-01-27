package fieldlinker;

public interface AsMap_slots {
    Object setSlot(int element, double value);

    Object setSlot(int element, int value);

    // hopefully these are temp, the whole point of having slots was to avoid Doubles and casting
    default Object setSlot(Object element, double value) {
        return setSlot(((Number) element).intValue(), value);
    }

    default Object setSlot(Object element, int value) {
        return setSlot(((Number) element).intValue(), value);
    }

    double getSlot(int element);

    default Object getSlot(Object element) {
        return getSlot(((Number) element).intValue());
    }
}