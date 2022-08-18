package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Container Widget to dynamically manage the organization of gui widgets. The Direction of this object is the
 * visual organization of the object it contains, i.e. topdown for Vertical and left-right for Horizontal.
 * The Alignment of the object is where the gui elements encapsulated will be centered on. So a Vertical,
 * START_ALIGNED Directional Frame will render all of its elements at the top of the screen, going down.
 *
 * Encapsulated Frames can be added within other frames.
 *
 * TODO: Make the Screen#addWidget call unnecessary by passing along GuiEventListener functions to the visible
 *  children. Should reduce lag of maintaining all the children widgets.
 *
 *  TODO: Height and Width need to be usable when creating the children DirectionalFrames. As it is, directionalFrames
 *   require the screen height and width instead of the parent's.
 *
 *  TODO: when setting a child widget to visible or invisible, need to manually call recalculateSize on the parent Frame.
 *
 * Additionally, new AbstractWidgets were added to assist this Frame.
 * @see TextWidget
 * @see SpacerWidget
 *
 * @see DirectionalFrame#createHorizontalFrame(int, Alignment)
 * @see DirectionalFrame#createVerticalFrame(int, Alignment)
 */
public class DirectionalFrame extends AbstractWidget{
    public static final int MINIMUM_PADDING = 5;

    public enum Alignment {
        START_ALIGNED,
        CENTERED,
        END_ALIGNED,
        EQUALLY_SPACED
    }

    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }

    private final Alignment alignment;
    private final Direction direction;
    private final DirectionalWidget directionalWidget;
    private int numSpacers;
    private final List<Integer> padding;
    private boolean sizeChanged;
    private final List<AbstractWidget> widgets;

    /**
     * Interface for generalizing the standard operations when calculating size and position along
     * the "main" axis and the "secondary" axis.
     * main - Direction
     * secondary - The other Direction.
     */
    interface DirectionalWidget {
        int getSizeAlongAxis(AbstractWidget widget);
        void setSecondaryAxisPos(AbstractWidget widget);
        void setMainAxisPos(AbstractWidget widget, int pos);
        void setSizeAlongAxis(AbstractWidget widget, int size);
    }

    public DirectionalFrame(int x, int y , int width, int height, Direction direction, Alignment alignment) {
        super(x, y, width, height, new TextComponent(""));
        this.widgets = new ArrayList<>();
        this.numSpacers = 0;
        this.padding = new ArrayList<>();
        this.direction = direction;
        this.sizeChanged = false;
        this.alignment = alignment;
        if (direction == Direction.HORIZONTAL) {
            this.directionalWidget = new DirectionalWidget() {

                @Override
                public int getSizeAlongAxis(AbstractWidget widget) {
                    return widget.getWidth();
                }

                @Override
                public void setSecondaryAxisPos(AbstractWidget widget) {
                    widget.y = DirectionalFrame.this.y;
                }

                @Override
                public void setMainAxisPos(AbstractWidget widget, int pos) {
                    widget.x = pos;
                }

                @Override
                public void setSizeAlongAxis(AbstractWidget widget, int size) {
                    widget.setWidth(size);
                }
            };
        } else {
            this.directionalWidget = new DirectionalWidget() {

                @Override
                public int getSizeAlongAxis(AbstractWidget widget) {
                    return widget.getHeight();
                }

                @Override
                public void setSecondaryAxisPos(AbstractWidget widget) {
                    widget.x = DirectionalFrame.this.x;
                }

                @Override
                public void setMainAxisPos(AbstractWidget widget, int pos) {
                    widget.y = pos;
                }

                @Override
                public void setSizeAlongAxis(AbstractWidget widget, int size) {
                    widget.setHeight(size);
                }
            };
        }
    }

    /**
     * Insert a SpacerWidget at the end of the frame. Overrides the alignment of the frame.
     * @see SpacerWidget
     */
    public void addSpacer() {
        this.widgets.add(new SpacerWidget(0, this.y));
        this.padding.add(0);
        this.numSpacers += 1;
        sizeChanged = true;
    }

    /**
     * Add the widget to this Frame. Widgets added this way should NOT be added to the screen with
     * `Screen.addRenderableWidget`. If signals/ functions are required from the object, use `Screen.addWidget`
     * method instead.
     *
     * @param widget The widget to add.
     */
    public void addWidget(AbstractWidget widget) {
        directionalWidget.setSecondaryAxisPos(widget);
        this.widgets.add(widget);
        this.padding.add(MINIMUM_PADDING);
        sizeChanged = true;
    }

    /**
     * Add the widget to this Frame. Widgets added this way should NOT be added to the screen with
     * `addRenderableWidget`. If signals/ functions are required from the object, use `Screen.addWidget`
     * method instead.
     * @param widget The widget to add.
     * @param padding The padding in front and behind to add. (is effectively doubled).
     */
    public void addWidget(AbstractWidget widget, int padding) {
        directionalWidget.setSecondaryAxisPos(widget);
        this.widgets.add(widget);
        this.padding.add(padding);
        sizeChanged = true;
    }

    public boolean charTyped(char key, int p_94733_) {
        if (this.active && this.isAnyVisible()) {
            for (AbstractWidget widget : widgets) {
                if (widget.visible && widget.charTyped(key, p_94733_)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean changeFocus(boolean p_94756_) {
        if (this.active && this.isAnyVisible()) {
            for (AbstractWidget widget : widgets) {
                if (widget.visible && widget.changeFocus(p_94756_)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create a horizontal Frame for AbstractWidgets.
     * @param frameWidth The maximum available width for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createHorizontalFrame(int frameWidth, Alignment alignment) {
        return new DirectionalFrame(0, 0, frameWidth, 0, Direction.HORIZONTAL, alignment);
    }

    /**
     * Create a vertical Frame for AbstractWidgets.
     * @param frameHeight The maximum available height for this widget.
     * @return The vertical directional frame.
     */
    public static DirectionalFrame createVerticalFrame(int frameHeight, Alignment alignment) {
        return new DirectionalFrame(0, 0, 0,frameHeight, Direction.VERTICAL, alignment);
    }

    public int getHeight() {
        int height = 0;
        if (direction == Direction.HORIZONTAL) {
            for (AbstractWidget widget : widgets) {
                height = Math.max(widget.getHeight(), height);
            }
        } else {
            height = getMinimumSize();
        }
        return height;
    }

    /**
     * Calculate the minimum size used by this DirectionalFrame. Does not account for Spacers.
     * @return the minimum size.
     */
    public int getMinimumSize() {
        int size = 0;
        for(int i = 0; i < this.widgets.size(); i++) {
            AbstractWidget widget = this.widgets.get(i);
            if (widget.visible) {
                if (widget instanceof DirectionalFrame && !((DirectionalFrame) widget).isAnyVisible()) {
                    continue;
                } else if (widget instanceof SpacerWidget) {
                    continue;
                }
                size += directionalWidget.getSizeAlongAxis(widgets.get(i)) + (2 * this.padding.get(i));
            }
        }
        return size;
    }

    public int getWidth() {
        int width = 0;
        if (direction == Direction.VERTICAL) {
            for (AbstractWidget widget : widgets) {
                width = Math.max(widget.getWidth(), width);
            }
        } else {
            width = getMinimumSize();
        }
        return width;
    }

    /**
     * Insert this widget to this Frame at a certain index. Widgets added this way should NOT be added to
     * the screen with `addRenderableWidget`. If signals/ functions are required from the object, use the
     * `Screen.addWidget` method instead.
     * @param widget The widget to add.
     * @param index The index at which to add this widget.
     * @param padding The padding in front and behind to add. (is effectively doubled).
     */
    public void insertWidget(AbstractWidget widget, int index, int padding) {
        directionalWidget.setSecondaryAxisPos(widget);
        this.widgets.add(index, widget);
        this.padding.add(index, padding);
        sizeChanged = true;
    }

    /**
     * Iterates over the children widgets of this Frame and checks if any are visible.
     *
     * @return True if any visible.
     */
    public boolean isAnyVisible() {
        if (!this.visible) return false;
        for (AbstractWidget widget : widgets) {
            if (widget.visible && !(widget instanceof SpacerWidget)) return true;
        }
        return false;
    }
/*       TODO: Implement below. As is does not remove the addWidget requirement.
//    public boolean isMouseOver(double mouseX, double mouseY) {
//        if (this.active && this.isAnyVisible()) {
//            if (mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)this.x + this.width && mouseY < (double)this.y + this.height) {
//                for (AbstractWidget widget : widgets) {
//                    if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//
//    @Override
//    public boolean keyPressed(int key, int scanCode, int modifiers) {
//        if (this.active && this.isAnyVisible()) {
//            for (AbstractWidget widget : widgets) {
//                if (widget.visible && widget.keyPressed(key, scanCode, modifiers)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean keyReleased(int key, int scanCode, int modifiers) {
//        if (this.active && this.isAnyVisible()) {
//            for (AbstractWidget widget : widgets) {
//                if (widget.visible && widget.keyReleased(key, scanCode, modifiers)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//
//    @Override
//    public void mouseMoved(double mouseX, double mouseY) {
//        if (this.active && this.isAnyVisible()) {
//            for (AbstractWidget widget : widgets) {
//                if (widget.visible) {
//                    widget.mouseMoved(mouseX, mouseY);
//                }
//            }
//        }
//    }
//
//    @Override
//    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//        if (this.active && this.isAnyVisible()) {
//            if (mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)this.x + this.width && mouseY < (double)this.y + this.height) {
//                for (AbstractWidget widget : widgets) {
//                    if (widget.visible && widget.mouseClicked(mouseX, mouseY, mouseButton)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
//        if (this.active && this.isAnyVisible()) {
//            if (mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)this.x + this.width && mouseY < (double)this.y + this.height) {
//                for (AbstractWidget widget : widgets) {
//                    if (widget.visible && widget.mouseReleased(mouseX, mouseY, mouseButton)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double mouseToX, double mouseToY) {
//        if (this.active && this.isAnyVisible()) {
//            if (mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)this.x + this.width && mouseY < (double)this.y + this.height) {
//                for (AbstractWidget widget : widgets) {
//                    if (widget.visible && widget.mouseDragged(mouseX, mouseY, mouseButton, mouseToX, mouseToY)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//        if (this.active && this.isAnyVisible()) {
//            if (mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)this.x + this.width && mouseY < (double)this.y + this.height) {
//                for (AbstractWidget widget : widgets) {
//                    if (widget.visible && widget.mouseScrolled(mouseX, mouseY, amount)) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }
 */


    /**
     * Recalulate the spacing of the widgets.
     */
    public void recalculateSize() {
        this.sizeChanged = false;
        int emptySpaceSize, nextSpot, spacerSize;
        int minimumSize = this.getMinimumSize();

        if (direction == Direction.HORIZONTAL) {
            nextSpot = this.x;
            if (minimumSize > this.width) {
                emptySpaceSize = minimumSize;
            } else {
                emptySpaceSize = this.width - minimumSize;
            }
        } else {
            nextSpot = this.y;
            if (minimumSize > this.height) {
                emptySpaceSize = minimumSize;
            } else {
                emptySpaceSize = this.height - minimumSize;
            }
        }
        if (numSpacers > 0) {
            spacerSize = (emptySpaceSize - nextSpot) / numSpacers;
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (widget instanceof SpacerWidget) {
                    directionalWidget.setSizeAlongAxis(widget, spacerSize);
                } else if (!widget.visible) {
                    continue;
                }
                nextSpot += this.padding.get(i);
                directionalWidget.setMainAxisPos(widget, nextSpot);
                nextSpot += directionalWidget.getSizeAlongAxis(widget) + this.padding.get(i);
            }
        } else {
            int whiteSpaceSize = 0;
            switch (alignment) {
                case CENTERED -> nextSpot += emptySpaceSize / 2;
                case END_ALIGNED -> nextSpot += emptySpaceSize;
                case EQUALLY_SPACED -> {
                    whiteSpaceSize = (emptySpaceSize - nextSpot) / (this.widgets.size() + 1);
                    nextSpot += whiteSpaceSize;
                }
            }
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                if (!widget.visible) {
                    continue;
                }
                nextSpot += this.padding.get(i);
                directionalWidget.setMainAxisPos(widget, nextSpot);
                nextSpot += directionalWidget.getSizeAlongAxis(widget) + this.padding.get(i) + whiteSpaceSize;
            }
        }
    }

    public void render(@NotNull PoseStack poseStack, int x, int y, float partialTicks) {
        if (this.visible) {
            if (sizeChanged) recalculateSize();
            for (AbstractWidget widget : widgets) {
                if (widget.visible) {
                    directionalWidget.setSecondaryAxisPos(widget);
                    widget.render(poseStack, x, y, partialTicks);
                }
            }
        }
    }

    /**
     * Recalculate the size of all inner frames in preparation of redraw.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        this.recalculateSize();
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput p_169152_) {

    }
}
