package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class DirectionalFrame extends AbstractWidget{
    public static final boolean HORIZONTAL = true;
    public static final boolean VERTICAL = false;
    public static final int MINIMUM_PADDING = 5;

    private boolean direction;
    private int minimumSize, numSpacers;
    private final List<Integer> padding;
    private boolean sizeChanged;
    private final List<AbstractWidget> widgets;

    public DirectionalFrame(int x, int y , int width, int height, boolean direction) {
        super(x, y, width, height, new TextComponent(""));
        this.widgets = new ArrayList<>();
        this.minimumSize = 0;
        this.numSpacers = 0;
        this.padding = new ArrayList<>();
        this.direction = direction;
        this.sizeChanged = false;
    }

    public void addSpacer() {
        this.widgets.add(new SpacerWidget(0, this.y));
        this.padding.add(0);
        this.numSpacers += 1;
    }

    public void addWidget(AbstractWidget widget) {
        if (this.direction) {
            widget.y = this.y;
            this.minimumSize += widget.getWidth();
        }
        else {
            widget.x = this.x;
            this.minimumSize += widget.getHeight();
        }
        this.widgets.add(widget);
        this.padding.add(2*MINIMUM_PADDING);
        this.minimumSize += (2*MINIMUM_PADDING);

        sizeChanged = true;
    }

    public void addWidget(AbstractWidget widget, int padding) {
        if (this.direction) {
            widget.y = this.y;
            this.minimumSize += widget.getWidth();
        }
        else {
            widget.x = this.x;
            this.minimumSize += widget.getHeight();
        }
        padding *= 2;
        this.widgets.add(widget);
        this.padding.add(padding);
        this.minimumSize += padding;
        sizeChanged = true;
    }

    public static DirectionalFrame createHorizontalFrame(int x, int y, int frameWidth) {
        return new DirectionalFrame(x, y, frameWidth, 0, HORIZONTAL);
    }

    public static DirectionalFrame createVerticalFrame(int x, int y, int frameHeight) {
        return new DirectionalFrame(x, y, 0,frameHeight, VERTICAL);
    }

    /**
     * Recalulate the spacing of the widgets.
     */
    public void recalulateSize() {
        if (this.direction) recalulateSizeHorizontal();
        else recalulateSizeVertical();
    }

    /**
     * Recalulate the spacing of the widgets.
     */
    public void recalulateSizeHorizontal() {
        this.sizeChanged = false;
        int spacerSize = 0;
        int nextSpot = MINIMUM_PADDING + this.x;
        if (numSpacers > 0) {
            spacerSize = (this.width - this.minimumSize) / numSpacers;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (widget instanceof SpacerWidget) {
                    widget.setWidth(spacerSize);
                }

                widget.x = nextSpot;

                nextSpot += widget.getWidth() + this.padding.get(i);
            }
        } else {
            int whiteSpaceSize = (this.width - (this.minimumSize + MINIMUM_PADDING)) / (this.widgets.size() + 1);
            nextSpot += whiteSpaceSize;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                widget.x = nextSpot;
                nextSpot += widget.getWidth() + this.padding.get(i) + whiteSpaceSize;
            }
        }
    }

    /**
     * Recalulate the spacing of the widgets.
     */
    public void recalulateSizeVertical() {
        this.sizeChanged = false;
        int spacerSize = 0;
        int nextSpot = MINIMUM_PADDING + this.y;
        if (numSpacers > 0) {
            spacerSize = (this.height - this.minimumSize) / numSpacers;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (widget instanceof SpacerWidget) {
                    widget.setHeight(spacerSize);
                }

                widget.y = nextSpot;
                nextSpot += widget.getHeight() + this.padding.get(i);
            }
        } else {
            int whiteSpaceSize = (this.width - (this.minimumSize + MINIMUM_PADDING)) / (this.widgets.size() + 1);
            nextSpot += whiteSpaceSize;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                widget.y = nextSpot;
                nextSpot += widget.getHeight() + this.padding.get(i) + whiteSpaceSize;
            }
        }
    }

    interface SetPosFunction {
        void setPos(AbstractWidget widget);
    }

    public void render(PoseStack poseStack, int x, int y, float partialTicks) {
        if (this.visible) {
            if (sizeChanged) recalulateSize();
            SetPosFunction func;
            if (direction) func = (widget) -> widget.y = this.y;
            else func = (widget) -> widget.x = this.x;
            for (AbstractWidget widget : widgets) {
                if (widget instanceof DirectionalFrame) ((DirectionalFrame) widget).setVisible(true);
                else widget.visible = true;
                func.setPos(widget);
                widget.render(poseStack, x, y, partialTicks);
            }
        }
    }

    public void resize(int width, int height) {
        sizeChanged = true;
        this.minimumSize = 0;
        for(AbstractWidget widget : widgets) this.minimumSize += widget.getWidth();
    }

    public void setVisible(boolean visible) {
        for(AbstractWidget widget : widgets) widget.visible = visible;
        this.visible = visible;
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
