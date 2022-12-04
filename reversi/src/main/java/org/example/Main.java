package org.example;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

public class Main {
	private static final int FIELD_SIZE = 8;
	private static final int EMPTY_CELL_VALUE = 0;
	private static final int WHITE_COLOR_VALUE = 1;
	private static final int BLACK_COLOR_VALUE = 2;
	private static final int MOVES_AVAILABLE_VALUE = 3;
	private int[][] field;
    private int[][] fieldSnapshot;
	private int emptyCellsAmount;
	private int blackCellsAmount;
	private int chosenMode;
	private int highscore;
	private int whiteCellsAmount;
	private final Map<Integer, Double> AIMap;
    private final Scanner scn;
	private boolean isWhiteTurn;

	public Main() {
		field = new int[FIELD_SIZE][FIELD_SIZE];
		scn = new Scanner(System.in);
		AIMap = new HashMap<>();
		boolean isPlaying = true;
		reset();
		render();
		while(isPlaying) {
			try {
				var line = scn.nextLine().split(" ");
				if("z".equals(line[0])) {
					undo();
					render();
                } else if("q".equals(line[0])) isPlaying = false;
				else {
                    var x = Integer.parseInt(line[0]);
                    var y = Integer.parseInt(line[1]);
                    if(!isInbound(x) || !isInbound(y)) throw new Exception();
                    isWhiteTurn = !isWhiteTurn;
                    if (makeTurn(x, y)) {
                        render();
						switch (chosenMode) {
							case 1 -> lowAITurn();
							case 2 -> smartAITurn();
							case 3 -> isWhiteTurn = !isWhiteTurn;
							default -> {}
						}
                    } else System.out.println("This move is illegal");
                }
            } catch(Exception e) {
				System.out.println("Illegal input");
            }
		}
	}

	private void undo() {
		field = fieldSnapshot;
	}


	private void reset() {
		System.out.printf("Current Highscore: %s%n", highscore);
        System.out.println("""
        Choose Mode:
        1) Easy Bot
        2) Smart Bot
        3) PvP
        """);
        chosenMode = scn.nextInt();
        scn.nextLine();
		for(var e : field) Arrays.fill(e, EMPTY_CELL_VALUE);
		field[3][3] = WHITE_COLOR_VALUE;
        field[4][4] = WHITE_COLOR_VALUE;
        field[3][4] = BLACK_COLOR_VALUE;
        field[4][3] = BLACK_COLOR_VALUE;
        blackCellsAmount = 2;
        whiteCellsAmount = 2;
        emptyCellsAmount = 60;
        isWhiteTurn = true;
        render();
	}

	private void gameOver() {
		if(blackCellsAmount > whiteCellsAmount) {
			System.out.println("Black Win");
			highscore = blackCellsAmount;
		} else if(chosenMode < 3) System.out.println("AI Win");
		else {
		    System.out.println("White Win");
		    highscore = whiteCellsAmount;
        }
		reset();
	}

	private void lowAITurn() {
		getLowAIMoves(true, AIMap);
		makeAITurn();
	}

	private void smartAITurn() {
	    getSmartAIMoves();
	    makeAITurn();
	}

	private void makeAITurn() {
		var maxEntry = getMaxEntry(AIMap);
        if(maxEntry.isEmpty()) gameOver();
        else {
            var index = maxEntry.get().getKey();
            tryPlace(index % FIELD_SIZE, index / FIELD_SIZE, null, false);
            AIMap.clear();
            getLowAIMoves(false, AIMap);
            if(AIMap.isEmpty()) gameOver();
            else {
                System.out.println("All available move for player:");
                for(var e : AIMap.keySet()) {
                    int x = e % FIELD_SIZE;
                    int y = e / FIELD_SIZE;
                    System.out.printf("(%s, %s)%n", x, y);
                    field[x][y] = MOVES_AVAILABLE_VALUE;
                }
            }
            AIMap.clear();
            render();
        }
	}

	private void getSmartAIMoves() {
        getLowAIMoves(true, AIMap);
        Map<Integer, Double> tmp = new HashMap<>();
        AIMap.replaceAll((index, value) -> {
            tmp.clear();
			int x = index % FIELD_SIZE;
			int y = index / FIELD_SIZE;
			int old = field[x][y];
			field[x][y] = WHITE_COLOR_VALUE;
            getLowAIMoves(false, tmp);
            field[x][y] = old;
            return value - getMaxEntry(tmp).orElseGet(() -> new SimpleEntry<>(0, 0.0)).getValue();
        });
	}

	private void getLowAIMoves(boolean isWhite, Map<Integer, Double> out) {
		boolean old = isWhiteTurn;
		isWhiteTurn = isWhite;
		for (var x = 0; x < FIELD_SIZE; x++) {
            for (var y = 0; y < FIELD_SIZE; y++) {
                if (field[x][y] == 0) tryPlace(x, y, out, true);
            }
        }
        isWhiteTurn = old;
	}

	private boolean tryPlace(int x, int y, Map<Integer, Double> out, boolean isAITurn) {
		int enemyColor = isWhiteTurn ? BLACK_COLOR_VALUE : WHITE_COLOR_VALUE;
		int ownColor = isWhiteTurn ? WHITE_COLOR_VALUE : BLACK_COLOR_VALUE;
		var flag = false;
		var R = 0.0;
		for (var i = -1; i <= 1; i++) {
			if (x + i < 0 || x + i >= FIELD_SIZE) continue;
			for (var j = -1; j <= 1; j++) {
				if (y + j < 0 || y + j >= FIELD_SIZE) continue;
				if (i == 0 && j == 0) continue;
				var xx = x + i;
				var yy = y + j;
				if (field[xx][yy] == enemyColor) {
					xx += i;
                    yy += j;
					while (isInbound(xx) && isInbound(yy)) {
						if (field[xx][yy] == ownColor) {
							flag = true;
							if (isAITurn) {
								var rr = R;
								out.compute(y * FIELD_SIZE + x, (k, v) -> (v == null ? 0.0 : v) + rr);
							} else grabCellsInLine(ownColor, x, y, xx, yy);
							break;
						}
						if (field[xx][yy] == EMPTY_CELL_VALUE) break;
						R += isOnEdge(xx, yy) ? 2 : 1;
						xx += i;
						yy += j;
					}
				}
			}
		}
		if (isAITurn) AIMap.computeIfPresent(y * FIELD_SIZE + x, (k, v) -> v + (isOnAngle(x, y) ? 0.8 : isOnEdge(x, y) ? 0.4 : 0));
		if (flag && !isAITurn) {
			emptyCellsAmount--;
			if (isWhiteTurn) whiteCellsAmount++;
			else blackCellsAmount++;
		}
		return flag;
	}

	private void grabCellsInLine(int color, int fromX, int fromY, int toX, int toY) {
		var dx = toX - fromX;
		var dy = toY - fromY;
		var taken = Math.max(Math.abs(dx), Math.abs(dy)) - 2;
		if (isWhiteTurn) {
			whiteCellsAmount += taken;
			blackCellsAmount -= taken;
		} else {
			whiteCellsAmount -= taken;
			blackCellsAmount += taken;
		}
		for (var i = 0; i < taken + 3; i++)
			field[fromX + i * (int) Math.signum(dx)][fromY + i * (int) Math.signum(dy)] = color;
	}

	private boolean makeTurn(int x, int y) {
		var copy = Arrays.stream(field).map(int[]::clone).toArray(int[][]::new);
		if (!tryPlace(x, y, AIMap, false)) return false;
		if (emptyCellsAmount == 0 || blackCellsAmount == 0 || whiteCellsAmount == 0) gameOver();
		isWhiteTurn = !isWhiteTurn;
		fieldSnapshot = copy;
		for(int i = 0; i < FIELD_SIZE; i++) {
		    for(int j = 0; j < FIELD_SIZE; j++) {
                if(field[i][j] == MOVES_AVAILABLE_VALUE) field[i][j] = EMPTY_CELL_VALUE;
            }
		}
		return true;
	}

	private void render() {
		System.out.println("   0 1 2 3 4 5 6 7");
		System.out.println("--------------------");
		for (var y = 0; y < FIELD_SIZE; y++) {
			System.out.print(y + "| ");
			for (var x = 0; x < FIELD_SIZE; x++) {
				var v = field[x][y];
				System.out.print((v == 0 ? ' ' : v == MOVES_AVAILABLE_VALUE ? '\'' : v == BLACK_COLOR_VALUE ? 'x' : 'o') + " ");
			}
			System.out.println("|");
		}
		System.out.println("--------------------");
	}

	private static Optional<Entry<Integer, Double>> getMaxEntry(Map<Integer, Double> map) {
		return map.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue));
	}

	private static boolean isOnEdge(int x, int y) {
		return x == 0 || x == FIELD_SIZE - 1 || y == 0 || y == FIELD_SIZE - 1;
	}

	private static boolean isOnAngle(int x, int y) {
		return (x == 0 || x == FIELD_SIZE - 1) && (y == 0 || y == FIELD_SIZE - 1);
	}

	private static boolean isInbound(int coord) {
		return coord >= 0 && coord < FIELD_SIZE;
	}

	public static void main(String[] args) {
		new Main();
	}
}