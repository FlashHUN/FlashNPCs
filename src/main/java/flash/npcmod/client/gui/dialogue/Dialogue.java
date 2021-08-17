package flash.npcmod.client.gui.dialogue;

import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.json.JSONArray;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class Dialogue {

  private String name;
  private String text;
  private String response;
  private String function;
  private List<Dialogue> children;
  private boolean isInitDialogue;

  public Dialogue(String name, String text, String response, String function, Dialogue[] children) {
    this.name = name;
    this.text = text;
    this.response = response;
    this.function = function;
    this.setChildren(children);
    this.isInitDialogue = name.equals(ClientDialogueUtil.INIT_DIALOGUE_NAME);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  public Dialogue[] getChildren() {
    return children.toArray(new Dialogue[0]);
  }

  public void setChildren(Dialogue[] children) {
    this.children = new ArrayList<>();
    for (Dialogue dialogue : children) {
      this.children.add(dialogue);
    }
  }

  public boolean isChild(Dialogue dialogue) {
    return this.children.contains(dialogue);
  }

  public void addChild(Dialogue dialogue) {
    if (!dialogue.equals(this)) {
      if (!this.children.contains(dialogue)) {
        this.children.add(dialogue);
      }
    }
  }

  public void removeChild(Dialogue dialogue) {
    if (isChild(dialogue)) {
      this.children.remove(this.children.indexOf(dialogue));
    }
  }

  public boolean isInitDialogue() {
    return isInitDialogue;
  }

  public static final Dialogue getInitDialogue(Dialogue[] dialogues) {
    for(int i = 0; i < dialogues.length; i++) {
      if (dialogues[i].getName().equals(ClientDialogueUtil.INIT_DIALOGUE_NAME)) {
        return dialogues[i];
      }
    }
    throw new InvalidParameterException("Could not get init dialogue");
  }

  public static final Dialogue[] multipleFromJSONObject(JSONObject object) {
    if (object.has("entries")) {
      JSONArray entries = object.getJSONArray("entries");
      Dialogue[] dialogues = new Dialogue[entries.length()];
      for (int i = 0; i < entries.length(); i++) {
        dialogues[i] = fromJSONObject(entries.getJSONObject(i));
      }
      return dialogues;
    } else {
      return new Dialogue[]{fromJSONObject(object)};
    }
  }

  public static final Dialogue fromJSONObject(JSONObject object) {
    Dialogue[] children = new Dialogue[0];
    if (object.has("children")) {
      JSONArray currentChildren = object.getJSONArray("children");
      children = new Dialogue[currentChildren.length()];
      for (int i = 0; i < currentChildren.length(); i++) {
        JSONObject currentChild = currentChildren.getJSONObject(i);
        Dialogue childDialogue = fromJSONObject(currentChild);
        children[i] = childDialogue;
      }
    }

    String name = object.getString("name");
    String text = object.getString("text");
    String response = object.has("response") ? object.getString("response") : "";
    String function = object.has("function") ? object.getString("function") : "";

    return new Dialogue(name, text, response, function, children);
  }

  public static Dialogue newDialogue() {
    return new Dialogue("newDialogueNode", "New Node", "", "", new Dialogue[0]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Dialogue dialogue = (Dialogue) o;
    return name.equals(dialogue.name) && text.equals(dialogue.text)
        && response.equals(dialogue.response) && function.equals(dialogue.response)
        && children.equals(dialogue.children);
  }
}
