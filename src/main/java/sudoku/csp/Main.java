package sudoku.csp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by youngbinkim on 2/13/16.
 */
public class Main {
    Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main (String ... args) {
        Main solver = new Main();
        List<Double> ans = new ArrayList<>();

        for (int i = 1; i <= 71; i++) {
            long start = System.currentTimeMillis();
            ans.add(solver.processFilesInDir(String.valueOf(i)));
            long end = System.currentTimeMillis();
            System.out.println("Time taken : " + (((end - start) * 1.0) / 1000));
        }

        System.out.println(ans.toString());
    }

    public double processFilesInDir(final String dirName) {
        File folder = new File(getClass().getClassLoader().getResource(dirName).getFile());
        double sum = 0;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                System.err.println("What is directory doing in here? " + fileEntry.getName());
            } else {
                sum += processFile(dirName + "/" + fileEntry.getName());
            }
        }
        double average = (sum / folder.listFiles().length);
        System.out.println("Average for dir : " + dirName + ": " + average);
        return average;
    }

    public double processFile(final String fileName) {
        List<List<Tile>> board = initialize(fileName);
        SudokuSolver solver = new SudokuSolver(board);
        return solver.run();
    }

    public static int SUDOKU_ROW_SIZE = 9;
    public static int SUDOKU_COL_SIZE = 9;


    private List<List<Tile>> initialize(String fileName) {
        List<List<Tile>> board = new ArrayList<>(SUDOKU_ROW_SIZE);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));


        //Read File Line By Line
        try {
            String strLine;

            List<Tile> boardRow ;
            Tile tile;

            int row = 0;
            while ((strLine = br.readLine()) != null)   {
                String[] tokens = strLine.split(" ");
                boardRow = new ArrayList<>();


                if (tokens.length == SUDOKU_COL_SIZE) {

                    for (int i = 0; i < SUDOKU_COL_SIZE; i++) {
                        tile = new Tile(Integer.parseInt(tokens[i]), row, i);
                        logger.debug("New tile .. val: {} i: {} , j:{} ", tile.getVal(), tile.getRow(), tile.getCol());
                        boardRow.add(tile);
                    }
                    row++;
                }
                board.add(boardRow);
                //System.out.println(strLine);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception occurred while initiating the problem .. " + e);
        }
        return board;
    }
}
