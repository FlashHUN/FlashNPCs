package flash.npcmod.client.gui.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.core.node.NodeData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Dialogue extends NodeData {
    private String text;
    private String response;

    public Dialogue(String name) {
        super(name, "", new Dialogue[0]);
        this.text = "";
        this.response = "";
    }

    public Dialogue(String name, String text, String response, String function, Dialogue[] children) {
        super(name, function, children);
        this.text = text;
        this.response = response;
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
        Dialogue dialogue = (Dialogue) o;
        return name.equals(dialogue.name) && text.equals(dialogue.text)
                && response.equals(dialogue.response) && function.equals(dialogue.function)
                && children.equals(dialogue.children);
    }

    /**
     * Create a new Dialogue from a json object.
     * @param object The json object.
     * @return The new dialogue.
     */
    public static Dialogue fromJSONObject(JsonObject object) {
        Dialogue[] children = new Dialogue[0];
        if (object.has("children")) {
            JsonArray currentChildren = object.getAsJsonArray("children");
            children = new Dialogue[currentChildren.size()];
            for (int i = 0; i < currentChildren.size(); i++) {
                JsonObject currentChild = currentChildren.get(i).getAsJsonObject();
                Dialogue childDialogue = fromJSONObject(currentChild);
                children[i] = childDialogue;
            }
        }

        String name = object.get("name").getAsString();
        String text = object.get("text").getAsString();
        String response = object.has("response") ? object.get("response").getAsString() : "";
        String function = object.has("function") ? object.get("function").getAsString() : "";

        return new Dialogue(name, text, response, function, children);
    }

    /**
     * Return the children in an array.
     *
     * @return Dialogue[]
     */
    public Dialogue[] getChildren() {
        Dialogue[] children = new Dialogue[this.children.size()];
        int i = 0;
        for (NodeData child: this.children)
            children[i++] = (Dialogue) child;
        return children;
    }

    /**
     * Get the text of the Dialogue.
     *
     * @return The text.
     */
    public String getText() {
        return text;
    }

    /**
     * Get the response of the Dialogue.
     *
     * @return The response.
     */
    public String getResponse() {
        return response;
    }

    /**
     * Create Dialogue array from the JSON object.
     *
     * @param object The json object
     * @return array of Dialogue.
     */
    public static Dialogue[] multipleFromJSONObject(JsonObject object) {
        if (object.has("entries")) {
            JsonArray entries = object.getAsJsonArray("entries");
            int dialogueCount = entries.size();
            if (object.has("dialogueCount")) {
                dialogueCount = object.get("dialogueCount").getAsInt();
            }

            Dialogue[] dialogues = new Dialogue[dialogueCount];
            dialogueCount = 0;
            for (int i = 0; i < entries.size() && dialogueCount < dialogues.length; i++) {
                JsonObject nodeJson = entries.get(i).getAsJsonObject();
                if (nodeJson.has("type") && !nodeJson.get("type").getAsString().equals("dialogue")) {
                    continue;
                }
                dialogues[dialogueCount] = fromJSONObject(nodeJson);
                dialogueCount += 1;
            }
            return dialogues;
        } else {
            return new Dialogue[]{fromJSONObject(object)};
        }
    }

    /**
     * Create a new dialogue.
     *
     * @return The new dialogue.
     */
    public static Dialogue newDialogue() {
        return new Dialogue("newDialogueNode", "New Node", "", "", new Dialogue[0]);
    }

    /**
     * Set the response of the Dialogue.
     *
     * @param response The response.
     */
    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * Set the text of the Dialogue.
     *
     * @param text The text.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Build a Json object of the NodeData.
     *
     * @return The json version of that nodeData
     */
    public JsonObject toJSON() {
        JsonObject dialogueObject = super.toJSON();

        dialogueObject.addProperty("text", text);
        dialogueObject.addProperty("response", response);
        return dialogueObject;
    }
}
