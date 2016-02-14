package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        recursivelySolve();
    }

    /**
     * remove domains for neighbours
     * @param neighbours
     * @param val
     */
    public boolean forwardCheck(final Set<Tile> neighbours, final int val) {
        boolean status = true;
        for (Tile tile : neighbours) {
            removeDomin(tile, val);
            logger.debug("Removing domain {} of tile i: {} , j:{} ", val, tile.getRow(), tile.getCol());
            if (!tile.isAssigned() && tile.getDomain().size() == 0) {
                logger.debug("Forward checking detected inconsistency.. " +
                        "domain empty for tile i : {}, j: {} .. removed domain {} "
                        , tile.getRow(), tile.getCol(), val);
                status = false;
            }
        }
        return status;
    }

    private boolean recursivelySolve() {
        if (board.getMrv().isEmpty())
            return true;

        Tile chosen;
        List<Tile> choices = findMostRestrained();
        if (choices == null)
            return true;

        if (choices.size() > 1) {
            chosen = findMostRestrictingVariable(choices);
        } else {
            chosen = choices.get(0);
        }
        final Set<Tile> neighbours = board.getNeighbours(chosen);
        final List<Integer> values = getValuesInOrder(chosen, neighbours); // get values following Most Constraining values Heuristic.


        // assign value
        for (int val : values) {
            logger.debug("Assign {} to i:{}, j:{}", val, chosen.getRow(), chosen.getCol());
            // check if consistent..
            if (checkConsistent(chosen, val, neighbours)) {
                chosen.setAssigned();
                chosen.setVal(val);

                // keep notes of affected neighbours to revert later
                List<Tile> affectedNeighbours = neighbours.stream().filter(tile ->
                    tile.getDomain().contains(val)).collect(Collectors.toList());

                if (forwardCheck(neighbours, val)) {
                    logger.debug("Forward check succeeded");
                    // recursive call
                    if (recursivelySolve()) {
                        return true;
                    }
                }

                // fail.. revert
                revertAssignment(chosen, neighbours, affectedNeighbours, val);
            }
        }
        board.enqueueTile(chosen);
        return false;
    }

    // todo: finish this part
    private void revertAssignment(Tile chosen, Set<Tile> neighbours, List<Tile> affectedNeighbours, int val) {
        chosen.setAssigned(false);
        neighbours.stream().forEach(tile -> tile.incrementNumUNeighbours());
        affectedNeighbours.stream().forEach(tile -> addDomain(tile, val));
    }

    private boolean checkConsistent(final Tile chosen, final int val, final Set<Tile> neighbours) {
        // check if any of neighbour has that value already..
        return neighbours.stream().noneMatch(tile -> tile.isAssigned() && tile.getVal() == val);
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

        final Tile finalMinTile = minTile;
        choices.stream().filter(tile1 -> !tile1.equals(finalMinTile)).forEach(tile2 -> {
            logger.debug("Putting back tile into the queue.. i : {} , j: {} "
                    , tile2.getRow(), tile2.getCol());
        });
        logger.debug("Most restraining variable i: {} , j:{} , min: {} ", minTile.getRow(), minTile.getCol(),
                min);
        return minTile;
    }

    public List<Tile> findMostRestrained() {
        List<Tile> retList = new ArrayList<>();

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


        logger.debug("size : {} ", board.getTile(2, 0).getDomain().size());

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

    private void removeDomin(Tile tile, int val) {
        if (!tile.isAssigned() && tile.getDomain().contains(val)) {
            board.dequeueTile(tile);
            tile.removeDomain(val);
            board.enqueueTile(tile);
        }
        int newNum = tile.decrementNumUNeighbours();
        assert(newNum >= 0);
        logger.debug(".. Domain size: {} New # of UNeighbours.. {} for tile i:{}, j:{}",
                tile.getDomain().size(), newNum, tile.getRow(), tile.getCol());
    }

    private void addDomain(Tile tile, int val) {
        board.dequeueTile(tile);
        tile.addDomain(val);
        board.enqueueTile(tile);
    }

    public List<Integer> getValuesInOrder(Tile chosen, Set<Tile> neighbours) {
        Map<Integer, Integer> counterMap = new HashMap<>();
        List<Integer> domainList = neighbours.stream()
                .filter(neighbour -> !neighbour.isAssigned())
                .flatMap(neighbour -> neighbour.getDomain().parallelStream())
                .filter(num -> chosen.getDomain().contains(num))
                .collect(Collectors.toList());

        domainList.stream().forEach(num ->
                counterMap.put(num, counterMap.getOrDefault(num, 0) + 1));


        counterMap.entrySet().stream().forEach(entry -> {
            logger.debug("Value : {} # occurrence in neighbours' domain :{} " +
                    "tile .. i:{} , j:{} " , entry.getKey(), entry.getValue(),
                    chosen.getRow(), chosen.getCol());
        });

        return counterMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
