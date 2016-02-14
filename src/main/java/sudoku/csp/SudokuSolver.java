package sudoku.csp;

import java.util.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by youngbinkim on 2/13/16.
 */
public class SudokuSolver {
    public static int STATUS_FAILURE = -1;

    Logger logger = LoggerFactory.getLogger(SudokuSolver.class);

    final PriorityQueue<Tile> mrv = new PriorityQueue(81, new TileComparator()); // to find most restrained value

    private List<List<Tile>> board; // board representation

    // this is just to make coding easier
    private Map<Integer, List<Tile>> section = new HashMap<>();

    public SudokuSolver(List<List<Tile>> board) {
        this.board = board;
        iterateBoard(tile -> {
            int sectionNo = calcaluateSectionNo(tile.getRow(), tile.getCol());
            tile.setSecNo(sectionNo);
            if (!tile.isAssigned()) {
                List<Tile> sec = section.getOrDefault(sectionNo, new ArrayList<>());
                sec.add(tile);
                section.put(sectionNo, sec);
                mrv.add(tile);
                logger.debug("Give sec no {} to tile.. i: {} , j:{} ", sectionNo, tile.getRow(), tile.getCol());
            }
        });
    }

    public void run() {
        // first remove constraints as there are values initially..

        iterateBoard(tile -> {
            if (tile.isAssigned()) {
                int row = tile.getRow();
                int col = tile.getCol();
                List<Tile> neighbours = getNeighbours(row, col, calcaluateSectionNo(row, col));
                forwardCheck(neighbours, tile.getVal());
            }
        });

        recursivelySolve();
    }

    /**
     * remove domains for neighbours
     * @param neighbours
     * @param val
     */
    private void forwardCheck(List<Tile> neighbours, int val) {
        for (Tile tile : neighbours) {
            removeDomin(tile, val);
            logger.debug("Removing domain {} of tile i: {} , j:{} ", val, tile.getRow(), tile.getCol());
        }
    }


    private boolean recursivelySolve() {
        if (mrv.isEmpty())
            return true;

        Tile chosen;
        List<Tile> choices = findMostRestrained();
        if (choices.size() > 1) {
            chosen = findMostRestrictingVariable(choices);
        } else {
            chosen = choices.get(0);
        }
        List<Integer> values = getValuesInOrder(); // get values following Most Constraining values Heuristic.

        // assign value
        for (int val : values) {
            // check if consistent..
            if (checkConsistent(chosen, val)) {
                chosen.setAssigned();
                chosen.setVal(val);

                if (forwardCheck(chosen.getRow(), chosen.getCol(), chosen.getSecNo(), val)) {
                    // recursive call
                    if (recursivelySolve()) {
                        return true;
                    }
                }

                // fail.. revert
                chosen.setAssigned(false);
            }
        }
        return false;
    }

    private void solve () {
        Stack<SudokuStepState> states = new Stack<>();

        List<Tile> choices = findMostRestrained();
        if (choices.size() > 1) {
            Tile chosen = findMostRestrictingVariable(choices);
            int value = findMostRestrictingValue();

            // assign value


        }
    }

    public Tile findMostRestrictingVariable(List<Tile> choices) {
        Tile tile = choices.get(0);
        Tile minTile = tile;
        int min = tile.getNumUNeighbours();

        for (int i = 0; i < choices.size(); i++) {
            tile = choices.get(i);

            if (min > tile.getNumUNeighbours()) {
                min = tile.getNumUNeighbours();
                minTile = tile;
            } else if (min == tile.getNumUNeighbours()) {
                if (tile.getRow() < minTile.getRow() ||
                        (tile.getRow() == minTile.getRow() && tile.getCol() < minTile.getCol())) {
                    minTile = tile;
                }
            }
        }
        logger.debug("Most restraining variable i: {} , j:{} , min: {} ", minTile.getRow(), minTile.getCol(),
                min);
        return minTile;
    }

    public List<Tile> findMostRestrained() {
        List<Tile> retList = new ArrayList<>();

        Tile tile = mrv.poll();
        int domainSize = tile.getDomain().size();
        logger.debug("Chosen most restrained variable i: {} , j : {}, domain size: {} ",
                tile.getRow(), tile.getCol(), domainSize);
        retList.add(tile);

        Tile peek = mrv.peek();
        // check if there are ties
        while (peek.getDomain().size() == domainSize) {
            logger.debug("Value equals to most restrained variable i: {} , j : {}, domain size: {} ",
                    peek.getRow(), peek.getCol(), peek.getDomain().size());

            retList.add(mrv.poll());

            peek = mrv.peek();
        }

        return retList;
    }

    private List<Tile> getNeighbours(int row, int col, int i) {
        return null;
    }

    private List<Tile> getTilesInSec(int secNo) {
        return new ArrayList<>(section.get(secNo));
    }

    private List<Tile> getTilesInRow(int row) {
        final List<Tile> ret = new ArrayList<>(Main.SUDOKU_COL_SIZE);
        for (int i = 0; i < Main.SUDOKU_COL_SIZE; i++) {
            ret.add(board.get(row).get(i));
        }
        return ret;
    }

    private List<Tile> getTilesInCol(int col) {
        final List<Tile> ret = new ArrayList<>(Main.SUDOKU_ROW_SIZE);
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            ret.add(board.get(i).get(col));
        }
        return ret;
    }

    private void removeDomin(Tile tile, int val) {
        if (!tile.isAssigned()) {
            mrv.remove(tile);
            tile.removeDomain(val);
            mrv.add(tile);
        }
        tile.decrementNumUNeighbours();
    }


    public int calcaluateSectionNo(int row, int col){
        return (row / 3) * 3 + (col / 3);
    }

    public void iterateBoard(final Consumer<Tile> tileConsumer) {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            for (int j = 0; j < Main.SUDOKU_COL_SIZE; j++) {
                tileConsumer.accept(board.get(i).get(j));
            }
        }
    }


    public void printBoard(List<List<Tile>> board) {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            for (int j = 0; j < Main.SUDOKU_COL_SIZE; j++) {
                System.out.print(board.get(i).get(j).getVal());
            }
            System.out.println("");
        }
    }
}
