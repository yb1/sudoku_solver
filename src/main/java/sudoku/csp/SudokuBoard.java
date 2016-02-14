package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by youngbinkim on 2/14/16.
 */
public class SudokuBoard {
    private List<List<Tile>> board; // board representation

    Logger logger = LoggerFactory.getLogger(SudokuBoard.class);

    public PriorityQueue<Tile> getMrv() {
        return mrv;
    }

    final PriorityQueue<Tile> mrv = new PriorityQueue(81, new TileComparator()); // to find most restrained value
    final SudokuSolver solver;
    // this is just to make coding easier
    private Map<Integer, List<Tile>> section = new HashMap<>();

    public SudokuBoard(final List<List<Tile>> board, final SudokuSolver solver) {
        this.board = board;
        this.solver = solver;

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

    public void init(){
        // first remove constraints as there are values initially..
        iterateBoard(tile -> {
            if (tile.isAssigned()) {
                Set<Tile> neighbours = getNeighbours(tile);
                solver.forwardCheck(neighbours, tile.getVal());
            }
        });

    }

    public void iterateBoard(final Consumer<Tile> tileConsumer) {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            for (int j = 0; j < Main.SUDOKU_COL_SIZE; j++) {
                tileConsumer.accept(board.get(i).get(j));
            }
        }
    }


    public int calcaluateSectionNo(int row, int col) {
        return (row / 3) * 3 + (col / 3);
    }


    private Set<Tile> getTilesInSec(int secNo) {
        return new HashSet<>(section.get(secNo));
    }

    private Set<Tile> getTilesInRow(int row) {
        final Set<Tile> ret = new HashSet<>(Main.SUDOKU_COL_SIZE);
        for (int i = 0; i < Main.SUDOKU_COL_SIZE; i++) {
            ret.add(board.get(row).get(i));
        }
        return ret;
    }

    private Set<Tile> getTilesInCol(int col) {
        final Set<Tile> ret = new HashSet<>(Main.SUDOKU_ROW_SIZE);
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            ret.add(board.get(i).get(col));
        }
        return ret;
    }

    public void printBoard() {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            for (int j = 0; j < Main.SUDOKU_COL_SIZE; j++) {
                System.out.print(board.get(i).get(j).getVal());
            }
            System.out.println("");
        }
    }

    public Set<Tile> getNeighbours(Tile chosen) {
        Set<Tile> neighbours = getTilesInSec(chosen.getSecNo());
        neighbours.addAll(getTilesInRow(chosen.getRow()));
        neighbours.addAll(getTilesInCol(chosen.getCol()));
        neighbours.remove(chosen);
        return neighbours;
    }

    public Tile getTile (int row, int col) {
        return board.get(row).get(col);
    }

    public void dequeueTile(Tile tile) {
        //logger.debug("Dequeuing tile i:{} j:{} ", tile.getRow(), tile.getCol());
        mrv.remove(tile);
    }

    public void enqueueTile(Tile tile) {
        //logger.debug("Enqueuing tile to queue i:{} j:{} ", tile.getRow(), tile.getCol());
        mrv.add(tile);
    }
}
