package flash.npcmod.core.pathing;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Path {

  // TODO figure out how this should work

  public BlockPos[] points;

  public Path(BlockPos startPos, BlockPos endPos) {
    this(new BlockPos[]{startPos, endPos});
  }

  public Path(BlockPos[] points) {
    this.points = points;
  }

  public BlockPos[] getPoints() {
    return points;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Path{");
    for (BlockPos pos : points) {
      sb.append("pos[").append(pos.getX()).append(";").append(pos.getY()).append(";").append(pos.getZ()).append("],");
    }
    String s = sb.toString();
    s = s.substring(0, s.length()-1);
    return s;
  }

  public static Path fromString(String s) {
    String pathString = s.substring(5);
    if (pathString.isEmpty()) return new Path(BlockPos.ZERO, BlockPos.ZERO);

    String[] positions = pathString.split(",");
    List<BlockPos> posList = new ArrayList<>();
    for (int i = 0; i < positions.length; i++) {
      String[] xyz = positions[i].substring(4, positions[i].length()-1).split(";");
      try {
        BlockPos pos = new BlockPos(Integer.valueOf(xyz[0]), Integer.valueOf(xyz[1]), Integer.valueOf(xyz[2]));
        posList.add(pos);
      } catch (Exception e) {
        return new Path(BlockPos.ZERO, BlockPos.ZERO);
      }
    }

    return new Path(posList.toArray(new BlockPos[0]));
  }
}
