package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by youngbinkim on 2/14/16.
 *
 * This class represent board of sudoku, and has many useful functions to be operated on the board
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

        // assign secition No to each of tiles..
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

    // get neighbours
    private Set<Tile> getTilesInSec(int secNo) {
        return new HashSet<>(section.get(secNo));
    }

    // get neighbours
    private Set<Tile> getTilesInRow(int row) {
        final Set<Tile> ret = new HashSet<>(Main.SUDOKU_COL_SIZE);
        for (int i = 0; i < Main.SUDOKU_COL_SIZE; i++) {
            ret.add(board.get(row).get(i));
        }
        return ret;
    }
    // get neighbours
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
        System.out.println("");
    }

    public Set<Tile> getNeighbours(Tile chosen) {
        Set<Tile> neighbours = getTilesInSec(chosen.getSecNo());
        neighbours.addAll(getTilesInRow(chosen.getRow()));
        neighbours.addAll(getTilesInCol(chosen.getCol()));
        neighbours.remove(chosen);
        return neighbours;
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

    public void verify() {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            for (int j = 0; j < Main.SUDOKU_COL_SIZE; j++) {
                Tile tile = board.get(i).get(j);
                if (!tile.isAssigned() || tile.getVal() == 0 ) {
                    throw new IllegalStateException("Tile " + tile.isAssigned() + " " + tile.getVal());
                }
            }
        }
        verifyRow();
        verifyCol();
        verifySec();
    }

    private void verifySec() {

        for (int i = 0; i < 9; i++) {
            final Set<Integer> seen = new HashSet<>();
            final int finalI = i;
            section.get(i).stream().forEach(tile1 -> {
                if (seen.contains(tile1.getVal()))
                    throw new IllegalStateException("INCONSISTENCY .. while checking section " + finalI);
                seen.add(tile1.getVal());
            });
        }
    }


    private void verifyRow() {
        for (int i = 0; i < Main.SUDOKU_ROW_SIZE; i++) {
            final Set<Integer> seen = new HashSet<>();
            final int finalI = i;
            board.get(i).stream().forEach(tile -> {
                if (seen.contains(tile.getVal()))
                    throw new IllegalStateException("INCONSISTENCY .. while checking row " + finalI);
                seen.add(tile.getVal());
            });
        }
    }


    private void verifyCol() {
        Set<Integer> seen;
        for (int i = 0; i < Main.SUDOKU_COL_SIZE; i++) {
            seen = new HashSet<>();
            for (int j = 0; j < Main.SUDOKU_ROW_SIZE; j++) {
                int val = board.get(j).get(i).getVal();
                if (seen.contains(val))
                    throw new IllegalStateException("INCONSISTENCY .. while checking col " + i);
                seen.add(val);
            }
        }
    }
}
