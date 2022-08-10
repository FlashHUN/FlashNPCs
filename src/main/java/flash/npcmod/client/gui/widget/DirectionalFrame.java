package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manage the position of the widgets contained within this frame. Buttons and widgets with signals require the
 * 'addWidget' call from the screen still.
 */
public class DirectionalFrame extends AbstractWidget{
    public static final boolean HORIZONTAL = true;
    public static final boolean VERTICAL = false;
    public static final int MINIMUM_PADDING = 5;

    public enum Alignment {
        START_ALIGNED,
        CENTERED,
        END_ALIGNED
    };

    private Alignment alignment;
    private final boolean direction;
    private int minimumSize, numSpacers;
    private final List<Integer> padding;
    private boolean sizeChanged;
    private final List<AbstractWidget> widgets;

    public DirectionalFrame(int x, int y , int width, int height, boolean direction, Alignment alignment) {
        super(x, y, width, height, new TextComponent(""));
        this.widgets = new ArrayList<>();
        this.minimumSize = 0;
        this.numSpacers = 0;
        this.padding = new ArrayList<>();
        this.direction = direction;
        this.sizeChanged = false;
        this.alignment = alignment;
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

    /**
     * Create a horizontal Frame for AbstractWidgets.
     * @param frameWidth The maximum available width for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createHorizontalFrame(int frameWidth, Alignment alignment) {
        return new DirectionalFrame(0, 0, frameWidth, 0, HORIZONTAL, alignment);
    }

    /**
     * Create a horizontal Frame for AbstractWidgets.
     * @param x The x coordinate of this frame.
     * @param y The y coordinate of this frame.
     * @param frameWidth The maximum available width for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createHorizontalFrame(int x, int y, int frameWidth, Alignment alignment) {
        return new DirectionalFrame(x, y, frameWidth, 0, HORIZONTAL, alignment);
    }

    /**
     * Create a vertical Frame for AbstractWidgets.
     * @param frameHeight The maximum available height for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createVerticalFrame(int frameHeight, Alignment alignment) {
        return new DirectionalFrame(0, 0, 0,frameHeight, VERTICAL, alignment);
    }

    /**
     * Create a vertical Frame for AbstractWidgets.
     * @param x The x coordinate of this frame.
     * @param y The y coordinate of this frame.
     * @param frameHeight The maximum available height for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createVerticalFrame(int x, int y, int frameHeight, Alignment alignment) {
        return new DirectionalFrame(x, y, 0,frameHeight, VERTICAL, alignment);
    }

    @Override
    public int getWidth() {
        if (direction) return this.minimumSize;
        else {
            return Collections.max(widgets, Comparator.comparing(AbstractWidget::getWidth)).getWidth();
        }
    }

    @Override
    public int getHeight() {
        if (!direction) return this.minimumSize;
        else {
            int max =  Collections.max(widgets, Comparator.comparing(AbstractWidget::getHeight)).getHeight();
            return max;
        }
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
        int spacerSize;
        int nextSpot = this.x;
        if (numSpacers > 0) {
            spacerSize = (this.width - this.minimumSize) / numSpacers;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (widget instanceof SpacerWidget) {
                    widget.setWidth(spacerSize);
                }

                widget.x = this.padding.get(i) + nextSpot;

                nextSpot += widget.getWidth() + this.padding.get(i);
            }
        } else {
            int whiteSpaceSize = (this.width - (this.minimumSize + MINIMUM_PADDING)) / (this.widgets.size() + 1);
            nextSpot += whiteSpaceSize;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                widget.x = this.padding.get(i) + nextSpot;
                nextSpot += widget.getWidth() + this.padding.get(i) + whiteSpaceSize;
            }
        }
    }

    /**
     * Recalulate the spacing of the widgets.
     */
    public void recalulateSizeVertical() {
        this.sizeChanged = false;
        int spacerSize;
        int nextSpot = this.y;
        if (numSpacers > 0) {
            spacerSize = (this.height - this.minimumSize) / numSpacers;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (widget instanceof SpacerWidget) {
                    widget.setHeight(spacerSize);
                }
                widget.y = this.padding.get(i) + nextSpot;
                nextSpot += widget.getHeight() + this.padding.get(i);
            }
        } else {
            int whiteSpaceSize = (this.height - (this.minimumSize + MINIMUM_PADDING)) / (this.widgets.size() + 1);
            nextSpot += whiteSpaceSize;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                widget.y = this.padding.get(i) + nextSpot;
                nextSpot += widget.getHeight() + this.padding.get(i) + whiteSpaceSize;
            }
        }
    }

    interface SetPosFunction {
        void setPos(AbstractWidget widget);
    }

    public void render(@NotNull PoseStack poseStack, int x, int y, float partialTicks) {
        if (this.visible) {
            if (sizeChanged) recalulateSize();
            SetPosFunction func;
            if (direction) func = (widget) -> widget.y = this.y;
            else func = (widget) -> widget.x = this.x;
            for (AbstractWidget widget : widgets) {
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
        this.visible = visible;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput p_169152_) {

    }
}
