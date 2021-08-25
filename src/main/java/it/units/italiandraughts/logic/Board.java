package it.units.italiandraughts.logic;


import it.units.italiandraughts.exception.IllegalMoveException;
import it.units.italiandraughts.ui.PieceType;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.beans.PropertyChangeSupport;

public class Board {
    private final Tile[][] tiles;
    public static final int SIZE = 8;
    private final PropertyChangeSupport support;

    public Board() {
        support = new PropertyChangeSupport(this);
        tiles = new Tile[SIZE][SIZE];

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Tile tile = new Tile(col, row);
                tiles[row][col] = tile;
            }
        }

        Arrays.stream(tiles).flatMap(Arrays::stream).filter(t -> t.getY() < 3 && (t.getY() + t.getX()) % 2 == 0)
                .forEach(t -> t.placePiece(new Piece(PieceType.PLAYER2)));
        Arrays.stream(tiles).flatMap(Arrays::stream).filter(t -> t.getY() > 4 && (t.getY() + t.getX()) % 2 == 0)
                .forEach(t -> t.placePiece(new Piece(PieceType.PLAYER1)));
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void move(int fromX, int fromY, int toX, int toY) {
        if ((toX + toY) % 2 == 1) {
            throw new IllegalMoveException("The required move is illegal because no piece can stand on a white tile");
        }
        Piece piece = tiles[fromY][fromX].getPiece();
        tiles[fromY][fromX].placePiece(null);
        tiles[toY][toX].placePiece(piece);
        notifyChange();
    }

    public void notifyChange() {
        support.firePropertyChange("board", null, tiles);
    }


    public void empty() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                tiles[i][j] = null;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Board{ board=\n");
        for (int i = 0; i < SIZE; i++) {
            result.append(Arrays.toString(tiles[i])).append("\n");
        }
        result.append(" }");
        return result.toString();
    }

    public Tile[][] getTiles() {
        return tiles;
    }
}
