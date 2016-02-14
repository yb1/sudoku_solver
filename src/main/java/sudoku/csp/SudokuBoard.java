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

    final SudokuSolver solver;
    // this is just to make coding easier
    private Map<Integer, List<Tile>> section = new HashMap<>();
    private Set<Tile> unassigned = new HashSet<>();

    public SudokuBoard(final List<List<Tile>> board, final SudokuSolver solver) {
        this.board = board;
        this.solver = solver;

        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            section.put(i, new ArrayList<>());
        }

        iterateBoard(tile -> {
            int sectionNo = calcaluateSectionNo(tile.getRow(), tile.getCol());
            tile.setSecNo(sectionNo);
            if (!tile.isAssigned()) {
                List<Tile> sec = section.get(sectionNo);
                sec.add(tile);
                section.put(sectionNo, sec);
                unassigned.add(tile);
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

    public Set<Tile> getUnassigned() {
        return unassigned;
    }

    public void addUnassigned(Tile tile) {
        unassigned.add(tile);
    }

    public List<Tile> getMostContrainedVariable(){
        final int min =
                unassigned.stream().mapToInt(tile -> tile.getDomain().size()).min().getAsInt();
        return unassigned.stream().filter(tile -> tile.getDomain().size() == min).collect(Collectors.toList());
    }

    public void removeUnassigned(Tile tile) {
        unassigned.remove(tile);
    }
}
