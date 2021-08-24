package it.units.italiandraughts.ui;

import it.units.italiandraughts.ItalianDraughts;
import it.units.italiandraughts.logic.Board;
import it.units.italiandraughts.logic.Piece;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.util.Arrays;

public class Drawer {
    private final Tile[][] tiles;
    private final Board board;

    protected static double getBoardHeight() {
        return ItalianDraughts.getScreenHeight() / 3 * 2;
    }

    public Drawer(GridPane gridPane, Board board) {
        tiles = new Tile[Board.SIZE][Board.SIZE];
        this.board = board;

        gridPane.setMinSize(getBoardHeight(), getBoardHeight());
        gridPane.setMaxSize(getBoardHeight(), getBoardHeight());

        double tileSize = gridPane.getMaxHeight() / Board.SIZE;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Tile square;
                if ((row + col) % 2 == 0) {
                    square = new Tile(this, TileType.BRONZE, row, col, tileSize);
                } else {
                    square = new Tile(this, TileType.WHITE_SMOKE, row, col, tileSize);
                }
                tiles[row][col] = square;
                gridPane.add(square, col, row);
            }
        }

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(12.5);
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(12.5);
        for (int i = 0; i < Board.SIZE; i++) {
            gridPane.getColumnConstraints().add(columnConstraints);
            gridPane.getRowConstraints().add(rowConstraints);
        }


    }

    public void draw() {
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                tiles[i][j].getChildren().clear();
                Piece piece = board.getBoard()[i][j];
                if (piece != null) {
                    new PieceDrawer(piece, tiles[i][j]).draw();
                    tiles[i][j].setEmpty(false);
                } else {
                    tiles[i][j].setEmpty(true);
                }
            }
        }
    }


    void markAsClicked(int x, int y) {
        Tile tile = tiles[x][y];
        if (tile.isEmpty()) {
            return;
        }
        Arrays.stream(tiles).flatMap(Arrays::stream).forEach(t -> t.highlight(false));
        tile.highlight(true);
    }

}
