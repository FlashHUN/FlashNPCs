package flash.npcmod.core;

public class ColorUtil {

  public static int ALPHA_MASK = 0xFF000000;
  public static int RED_MASK = 0xFF0000;
  public static int GREEN_MASK = 0xFF00;
  public static int BLUE_MASK = 0xFF;

  public static int[] hexToRgb(int hex) {
    int r = (hex & RED_MASK) >> 16;
    int g = (hex & GREEN_MASK) >> 8;
    int b = (hex & BLUE_MASK);
    return new int[]{r, g, b};
  }

  public static int[] hexToRgba(int hex) {
    int a = (hex & ALPHA_MASK) >> 24;
    int r = (hex & RED_MASK) >> 16;
    int g = (hex & GREEN_MASK) >> 8;
    int b = (hex & BLUE_MASK);
    return new int[]{r, g, b, a};
  }

  public static int rgbaToHex(int r, int g, int b, int a) {
    return (a << 24) | rgbToHex(r, g, b);
  }

  public static int rgbToHex(int r, int g, int b) {
    return (r << 16) | (g << 8) | b;
  }

  public static int rgbToHexA(int r, int g, int b) {
    return rgbaToHex(r, g, b, 255);
  }

  public static int hexToHexA(int hex) {
    return 0xFF000000 | hex;
  }

  public static int getColorMasked(int color, int mask) {
    return color & mask;
  }

  public static int hexToR(int hex) {
    return (hex & RED_MASK) >> 16;
  }

  public static int hexToG(int hex) {
    return (hex & GREEN_MASK) >> 8;
  }

  public static int hexToB(int hex) {
    return (hex & BLUE_MASK);
  }

}
