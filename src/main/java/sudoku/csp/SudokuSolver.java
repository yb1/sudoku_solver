package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by youngbinkim on 2/13/16.
 *
 * This is the class with main algorithm..
 *
 * Using fast tracking
 * Using most constrained variable, least restring value heuristic for selections..
 */
public class SudokuSolver {
    public static int STATUS_FAILURE = -1;

    Logger logger = LoggerFactory.getLogger(SudokuSolver.class);

    private SudokuBoard board; // board representation

    public SudokuSolver(List<List<Tile>> board) {
        this.board = new SudokuBoard(board, this);
        this.board.init();
    }

    public double run() {
        logger.debug("Run SudokuSolver");
        if (!recursivelySolve()){
            logger.error("No solution found..");
        }
        //board.printBoard();

        //board.verify();
        return numAssignment.get();
    }


    private AtomicInteger numAssignment = new AtomicInteger(0);


    /**
     * Main function to solve the sudoku recursively..
     * @return
     */
    private boolean recursivelySolve() {
        if (board.getUnassigned().isEmpty())
            return true;

        Tile chosen;
        List<Tile> choices = findMostRestricted();
        if (choices == null)
            return true;

        if (choices.size() > 1) {
            chosen = findMostRestrictingVariable(choices);
            logger.debug("Chosen.. i:{} j:{}", chosen.getRow(), chosen.getCol());
        } else {
            chosen = choices.get(0);
        }
        final Set<Tile> neighbours = board.getNeighbours(chosen);
        final List<Integer> values = getValuesInOrder(chosen, neighbours); // get values following Most Constraining values Heuristic.

        board.removeUnassigned(chosen);
        int numSteps;
        // assign value
        for (int val : values) {

            if ((numSteps = numAssignment.incrementAndGet()) > 10000) {
                return true; // this is to avoid taking too much time ..
            }

            logger.debug("Now has {} steps ", numSteps);
            logger.debug("Assign {} to i:{}, j:{}", val, chosen.getRow(), chosen.getCol());
            logger.debug("remaining " + board.getUnassigned().size());

            // check if consistent..
            if (checkConsistent(chosen, val, neighbours)) {
                chosen.setAssigned();
                chosen.setVal(val);

                // keep notes of affected neighbours to revert later
                Set<Tile> affectedNeighbours = neighbours.stream().filter(tile ->
                    !tile.isAssigned() && tile.getDomain().contains(val)).collect(Collectors.toSet());


                // forward check
                if (forwardCheck(affectedNeighbours, val)) {
                    logger.debug("Forward check succeeded");
                    // recursive call (next variable)
                    if (recursivelySolve()) {
                        return true;
                    }
                }

                // fail.. revert
                revertAssignment(chosen, neighbours, affectedNeighbours, val);
            } else {
                logger.debug("consistency failed " + val);
            }

        }
        logger.debug("Tile i:{} j:{} found no solution.. backtrack", chosen.getRow(), chosen.getCol());
        board.addUnassigned(chosen);
        return false;
    }


    /**
     * remove domains for neighbours
     * @param neighbours
     * @param val
     */
    public boolean forwardCheck(final Set<Tile> neighbours, final int val) {
        AtomicBoolean status = new AtomicBoolean(true);
        neighbours.stream().filter(tile -> !tile.isAssigned() && tile.getDomain().contains(val)).forEach(tile -> {
            removeDomain(tile, val);
            logger.debug("Removing domain {} of tile i: {} , j:{} ", val, tile.getRow(), tile.getCol());

            if (tile.getDomain().size() == 0) {
                logger.debug("Forward checking detected inconsistency.. " +
                                "domain empty for tile i : {}, j: {} .. removed domain {} "
                        , tile.getRow(), tile.getCol(), val);
                status.set(false);
            }
        });
        if (status.get() == false) {
            logger.debug("**Forward check failed !! {} " , val);
        }
        return status.get();
    }

    private void revertAssignment(Tile chosen, Set<Tile> neighbours, Set<Tile> affectedNeighbours, int val) {
        chosen.setAssigned(false);
        chosen.setVal(0);
        //neighbours.stream().forEach(tile -> tile.incrementNumUNeighbours());
        affectedNeighbours.stream().forEach(tile -> {
            logger.debug("Add {} val back into the tile i:{}, j:{}", val, tile.getRow(), tile.getCol() );
            addDomain(tile, val);
        });
    }

    private boolean checkConsistent(final Tile chosen, final int val, final Set<Tile> neighbours) {
        // check if any of neighbour has that value already..

        return neighbours.stream().noneMatch(tile -> {
            if (tile.isAssigned() && tile.getVal() == val) {
                logger.error(" MATCH !! i:{} j:{} {}", tile.getRow(), tile.getCol(), val);
            }
            return tile.isAssigned() && tile.getVal() == val;
        });
    }

    /**
     * Most Restricting Variable heuristic
     * @param choices
     * @return
     */
    public Tile findMostRestrictingVariable(List<Tile> choices) {
        return choices.stream().min(((o1, o2) -> {
            int numU1 = (int) board.getNeighbours(o1).stream().filter(tile -> !tile.isAssigned()).count();
            int numU2 = (int) board.getNeighbours(o2).stream().filter(tile -> !tile.isAssigned()).count();
            int res = Integer.compare(numU2, numU1);
            logger.debug("numU2 {} . tile i:{} j:{}", numU2, o2.getRow(), o2.getCol());
            if (res != 0)
                return res;
            else if (o1.getRow() != o2.getRow())
                return Integer.compare(o2.getRow(), o1.getRow());
            else
                return Integer.compare(o2.getCol(), o1.getCol());
        })).get();
    }

    /**
     * Most Constrained Variable heuristic
     * @return
     */
    public List<Tile> findMostRestricted() {
        List<Tile> retList = board.getMostContrainedVariable();

        retList.forEach(tile -> {
            logger.debug("Chosen most restricted variable i: {} , j : {}, domain size: {} ",
                    tile.getRow(), tile.getCol(), tile.getDomain().size());
        });

        return retList;
    }



    private void removeDomain(Tile tile, int val) {
        if (!tile.isAssigned() && tile.getDomain().contains(val)) {
            tile.removeDomain(val);
        }
        logger.debug(".. Domain size: {} for tile i:{}, j:{}",
                tile.getDomain().size(), tile.getRow(), tile.getCol());
    }

    private void addDomain(Tile tile, int val) {
        tile.addDomain(val);
    }

    /**
     * this is to get values following Least Restricting Value heuristic
     * @param chosen
     * @param neighbours
     * @return
     */
    public List<Integer> getValuesInOrder(Tile chosen, Set<Tile> neighbours) {
        final Map<Integer, Integer> counterMap = new HashMap<>();

        chosen.getDomain().stream().forEach(num -> {
            int count = (int) neighbours.stream().filter(tile -> tile.getDomain().contains(num)).count();
            counterMap.put(num, count);
        });

        counterMap.entrySet().stream().forEach(entry -> {
            logger.debug("Value : {} # occurrence in neighbours' domain :{} " +
                    "tile .. i:{} , j:{} " , entry.getKey(), entry.getValue(),
                    chosen.getRow(), chosen.getCol());
        });

        return counterMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
