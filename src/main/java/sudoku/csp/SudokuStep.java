package sudoku.csp;

import java.util.List;

/**
 * Created by youngbinkim on 2/13/16.
 */
public class SudokuStep {
    final SudokuStepState state = new SudokuStepState();
    final SudokuSolver problem;

    public SudokuStep(final SudokuSolver sudokuSolver) {
        problem = sudokuSolver;
    }


    public int run () {
        List<Tile> choices = problem.findMostRestrained();
        if (choices.size() > 1) {
            Tile chosen = problem.findMostRestrictingVariable(choices);
            int value = problem.findMostRestrictingValue();

            // assign value


        }
    }
}
