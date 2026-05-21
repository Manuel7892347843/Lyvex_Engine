package core.component.ui;

import core.component.Component;

public class Canvas extends Component {
    private int sortingOrder = 1000;
    private CanvasScalerMode scalerMode = CanvasScalerMode.SCALE_WITH_SCREEN_SIZE;
    private float referenceWidth = 1920.0f;
    private float referenceHeight = 1080.0f;
    private boolean visibleInSceneView = true;

    public int getSortingOrder() {
        return sortingOrder;
    }

    public void setSortingOrder(int sortingOrder) {
        this.sortingOrder = sortingOrder;
    }

    public CanvasScalerMode getScalerMode() {
        return scalerMode;
    }

    public void setScalerMode(CanvasScalerMode scalerMode) {
        this.scalerMode = scalerMode;
    }

    public float getReferenceWidth() {
        return referenceWidth;
    }

    public void setReferenceWidth(float referenceWidth) {
        this.referenceWidth = referenceWidth;
    }

    public float getReferenceHeight() {
        return referenceHeight;
    }

    public void setReferenceHeight(float referenceHeight) {
        this.referenceHeight = referenceHeight;
    }

    public boolean isVisibleInSceneView() {
        return visibleInSceneView;
    }

    public void setVisibleInSceneView(boolean visibleInSceneView) {
        this.visibleInSceneView = visibleInSceneView;
    }

    public float getScaleX(int viewportWidth) {
        if (scalerMode == CanvasScalerMode.CONSTANT_PIXEL_SIZE) {
            return 1.0f;
        }

        return viewportWidth / referenceWidth;
    }

    public float getScaleY(int viewportHeight) {
        if (scalerMode == CanvasScalerMode.CONSTANT_PIXEL_SIZE) {
            return 1.0f;
        }

        return viewportHeight / referenceHeight;
    }
}