package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by youngbinkim on 2/13/16.
 */
public class SudokuSolver {
    public static int STATUS_FAILURE = -1;

    Logger logger = LoggerFactory.getLogger(SudokuSolver.class);

    private SudokuBoard board; // board representation

    public SudokuSolver(List<List<Tile>> board) {
        this.board = new SudokuBoard(board, this);
        this.board.init();
    }

    public void run() {
        logger.debug("Run SudokuSolver");
        if (!recursivelySolve()){
            logger.error("No solution found..");
        }
        board.printBoard();
    }



    private AtomicInteger numAssignment = new AtomicInteger(0);

    private boolean recursivelySolve() {
        if (board.getUnassigned().isEmpty())
            return true;

        Tile chosen;
        List<Tile> choices = findMostRestricted();
        if (choices == null)
            return true;

        if (choices.size() > 1) {
            chosen = findMostRestrictingVariable(choices);
        } else {
            chosen = choices.get(0);
        }
        final Set<Tile> neighbours = board.getNeighbours(chosen);
        final List<Integer> values = getValuesInOrder(chosen, neighbours); // get values following Most Constraining values Heuristic.

        board.removeUnassigned(chosen);
        int numSteps;
        // assign value
        for (int val : values) {

            if ((numSteps = numAssignment.incrementAndGet()) > 10000)
                return true;

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


                if (forwardCheck(affectedNeighbours, val)) {
                    logger.debug("Forward check succeeded");
                    // recursive call
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
            if (tile.getRow() == 8 && tile.getCol() == 7 ) {
                String domainStr= tile.getDomain().stream().map(String::valueOf).collect(Collectors.joining(", "));
                logger.debug("@@@@@ {} ", domainStr);
            } else if (tile.getRow() == 8 && tile.getCol() == 3 ) {
                String domainStr= tile.getDomain().stream().map(String::valueOf).collect(Collectors.joining(", "));
                logger.debug("##### {} ", domainStr);
            }
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
            if (tile.getRow() == 8 && tile.getCol() == 7 ) {
                String domainStr= tile.getDomain().stream().map(String::valueOf).collect(Collectors.joining(", "));
                logger.debug("@@@@@ {} ", domainStr);
            } else if (tile.getRow() == 8 && tile.getCol() == 3 ) {
                String domainStr= tile.getDomain().stream().map(String::valueOf).collect(Collectors.joining(", "));
                logger.debug("##### {} ", domainStr);
            }
        });
    }

    private boolean checkConsistent(final Tile chosen, final int val, final Set<Tile> neighbours) {
        // check if any of neighbour has that value already..
        return true;
        /*
        return neighbours.stream().noneMatch(tile -> {
            if (tile.isAssigned() && tile.getVal() == val) {
                logger.error(" MATCH !! i:{} j:{} {}", tile.getRow(), tile.getCol(), val);
            }
            return tile.isAssigned() && tile.getVal() == val;
        });*/
    }

    public Tile findMostRestrictingVariable(List<Tile> choices) {
        return choices.stream().min(((o1, o2) -> {
            int numU1 = (int) board.getNeighbours(o1).stream().filter(tile -> !tile.isAssigned()).count();
            int numU2 = (int) board.getNeighbours(o2).stream().filter(tile -> !tile.isAssigned()).count();
            int res = Integer.compare(numU2, numU1);
            logger.debug("numU2 {} . tile i:{} j:{}", numU2, o2.getRow(), o2.getCol());
            if (res != 0)
                return res;
            else if (o1.getRow() != o2.getRow())
                return Integer.compare(o1.getRow(), o2.getRow());
            else
                return Integer.compare(o1.getCol(), o2.getCol());
        })).get();
        /*
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

        final Tile finalMinTile = minTile;
        choices.stream().filter(tile1 -> !tile1.equals(finalMinTile)).forEach(tile2 -> {
            logger.debug("Putting back tile into the queue.. i : {} , j: {} "
                    , tile2.getRow(), tile2.getCol());
        });
        logger.debug("Most restraining variable i: {} , j:{} , min: {} ", minTile.getRow(), minTile.getCol(),
                min);
                */
        //return minTile;
    }

    public List<Tile> findMostRestricted() {
        List<Tile> retList = board.getMostContrainedVariable();

        retList.forEach(tile -> {
            logger.debug("Chosen most restricted variable i: {} , j : {}, domain size: {} ",
                    tile.getRow(), tile.getCol(), tile.getDomain().size());
        });


        /*
        Tile tile =  board.getMrv().poll();
        int domainSize = tile.getDomain().size();
        logger.debug("Chosen most restrained variable i: {} , j : {}, domain size: {} ",
                tile.getRow(), tile.getCol(), domainSize);
        retList.add(tile);

        Tile peek = board.getMrv().peek();
        // check if there are ties
        while (peek != null && peek.getDomain().size() == domainSize) {
            logger.debug("Value equals to most restrained variable i: {} , j : {}, domain size: {} ",
                    peek.getRow(), peek.getCol(), peek.getDomain().size());

            retList.add(board.getMrv().poll());

            peek = board.getMrv().peek();
        }
*/

        //logger.debug("size : {} ", board.getTile(2, 0).getDomain().size());

        /*
        if (tile.getRow() == 1 && tile.getCol() == 4 && tile.getDomain().size() == 3) {
            while (peek != null) {
                logger.debug("LESSER Value equals to most restrained variable i: {} , j : {}, domain size: {} ",
                        peek.getRow(), peek.getCol(), peek.getDomain().size());

                retList.add(board.getMrv().poll());

                peek = board.getMrv().peek();
            }
            return null;
        }
*/
        return retList;
    }



    private void removeDomain(Tile tile, int val) {
        if (!tile.isAssigned() && tile.getDomain().contains(val)) {
            tile.removeDomain(val);
        }
        //int newNum = tile.decrementNumUNeighbours();
        //assert(newNum >= 0);
        logger.debug(".. Domain size: {} for tile i:{}, j:{}",
                tile.getDomain().size(), tile.getRow(), tile.getCol());
    }

    private void addDomain(Tile tile, int val) {
        tile.addDomain(val);
    }

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
