package flash.npcmod.client.gui.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parent of Behavior and Dialogue. Stores the data of a BuilderNode.
 *
 */
@OnlyIn(Dist.CLIENT)
abstract public class NodeData {

    protected String name;
    protected String function;
    protected List<NodeData> children;
    protected boolean isInitData;

    /**
     * Create the nodeData. Uses ClientDialogueUtil to get the string `init`.
     *
     * @param name The name.
     * @param function The function.
     * @param children The children.
     */
    public NodeData(String name, String function, NodeData[] children) {
        this.name = name;
        this.function = function;
        this.setChildren(children);
        this.isInitData = name.equals(ClientDialogueUtil.INIT_DIALOGUE_NAME);
    }

    /**
     * Adds the Child to the list and prevents adding itself.
     *
     * @param nodeData The node data.
     * @param index The option index.
     */
    public void addChild(NodeData nodeData, int index) {}

    /**
     * Adds the Child to the list and prevents adding itself.
     *
     * @param nodeData The node data.
     */
    public void addChild(NodeData nodeData) {
        if (!nodeData.equals(this)) {
            if (!this.children.contains(nodeData)) {
                this.children.add(nodeData);
            }
        }
    }

    /**
     * Compare this object to another.
     *
     * @param o The object.
     * @return The equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeData nodeData = (NodeData) o;
        return name.equals(nodeData.name) && function.equals(nodeData.function)
                && children.equals(nodeData.children);
    }

    /**
     * Get the name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the function of the node data.
     *
     * @return The function name.
     */
    public String getFunction() {
        return function;
    }

    /**
     * Get the children.
     *
     * @return The children.
     */
    abstract public NodeData[] getChildren();

    /**
     * Check if the nodeData is in the list of children.
     *
     * @param nodeData The node data.
     * @return boolean
     */
    public boolean isChild(NodeData nodeData) {
        return this.children.contains(nodeData);
    }

    /**
     * Check if `name` is `init`.
     *
     * @return boolean
     */
    public boolean isInitData() {
        return isInitData;
    }

    /**
     * Remove the nodeData from the list of children.
     *
     * @param nodeData The node object.
     */
    public void removeChild(NodeData nodeData) {
        if (isChild(nodeData)) {
            this.children.remove(nodeData);
        }
    }

    /**
     * Set Children to a new list.
     *
     * @param children The children node data.
     */
    public void setChildren(NodeData[] children) {
        this.children = new ArrayList<>();
        Collections.addAll(this.children, children);
    }

    /**
     * Set the name of this Node Data.
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the function.
     * @param function The function.
     */
    public void setFunction(String function) {
        this.function = function;
    }

    /**
     * Build a Json object of the NodeData.
     *
     * @return The json version of that nodeData
     */
    public JsonObject toJSON() {
        // Basic NodeData Properties
        String name = this.getName();
        if (name.length() == 0) {
            throw new InvalidParameterException("This node has no name. How did you even manage to do that?");
        }
        String function = this.getFunction();

        // Build our JSON Object from the properties
        JsonObject dialogueObject = new JsonObject();
        dialogueObject.addProperty("name", name);
        dialogueObject.addProperty("function", function);
        JsonArray childrenAsObjects = new JsonArray();
        for (NodeData child : this.getChildren()) childrenAsObjects.add(child.toJSON());

        dialogueObject.add("children", childrenAsObjects);

        return dialogueObject;
    }

    /**
     * Convert this object to a String.
     *
     * @return The string.
     */
    @Override
    public String toString() {
        return "NodeData{" +
                "name='" + name + '\'' +
                ", children=" + children +
                '}';
    }


}
