package sudoku.csp;

/**
 * Created by youngbinkim on 2/13/16.
 */
public class TileComparator implements java.util.Comparator<Tile> {
    @Override
    public int compare(Tile o1, Tile o2) {
        return o1.getDomain().size() - o2.getDomain().size();
    }
}
