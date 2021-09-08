package it.units.italiandraughts.logic;

import it.units.italiandraughts.exception.IllegalButtonClickException;
import it.units.italiandraughts.exception.IllegalMoveException;
import it.units.italiandraughts.ui.Drawer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.units.italiandraughts.logic.StaticUtil.matrixToStream;

public class Game {
    private Board board;
    private final Player player1;
    private final Player player2;
    private Player activePlayer;
    private Status status;
    private Tile activeTile;
    private Drawer drawer;
    private final PropertyChangeSupport support;
    private final List<int[]> log;
    private final MediaPlayer mediaPlayer;

    public Game(Board board, Player player1, Player player2) {
        this.board = board;
        this.player1 = player1;
        this.player2 = player2;
        this.activePlayer = player1;
        support = new PropertyChangeSupport(this);
        newTurn();
        log = new ArrayList<>();
        mediaPlayer = initMediaPlayer();
    }

    private void newTurn() {
        setActiveTile(null);
        setStatus(Status.IDLE);
        updateMovablePieces();
        List<Graph> graphs = matrixToStream(board.getTiles())
                .filter(tile -> !tile.isEmpty())
                .map(BlackTile::asBlackTile)
                .filter(tile -> tile.getPiece().getPieceColor().equals(activePlayer.getPieceColor())
                        && tile.getPiece().isMovable())
                .map(this::generateGraphForTile).collect(Collectors.toList());
        graphs.forEach(Graph::explorePossibleMoves);
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getActivePlayer() {
        return activePlayer;
    }

    public List<int[]> getLog() {
        return log;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    private void toggleActivePlayer() {
        final Player oldActivePlayer = activePlayer;
        if (player1.equals(activePlayer)) {
            activePlayer = player2;
        } else {
            activePlayer = player1;
        }
        support.firePropertyChange("activePlayer", oldActivePlayer, activePlayer);
    }

    public void move(int fromX, int fromY, int toX, int toY, boolean shouldLog) {
        if ((toX + toY) % 2 == 1) {
            throw new IllegalMoveException("The required move is illegal because no piece can stand on a white tile");
        }

        new Thread(() -> {
            mediaPlayer.play();
            mediaPlayer.seek(new Duration(0));
        }).start();

        Tile[][] tiles = getBoard().getTiles();
        BlackTile fromTile = (BlackTile) tiles[fromY][fromX];
        BlackTile toTile = (BlackTile) tiles[toY][toX];
        Piece piece = fromTile.getPiece();
        fromTile.removePiece();
        toTile.placePiece(piece);

        if (shouldLog) {
            log.add(IntStream.of(fromX, fromY, toX, toY).toArray());
        }

        toggleActivePlayer();
        newTurn();
    }

    private MediaPlayer initMediaPlayer() {
        String path = "src" + File.separatorChar + "main" + File.separatorChar + "resources" + File.separatorChar +
                "sounds" + File.separatorChar + "movePiece.mp3";
        File movePieceSoundFile = new File(path);
        MediaPlayer mediaPlayer = null;
        try {
            URL resource = movePieceSoundFile.toURI().toURL();
            Media media = new Media(resource.toString());
            mediaPlayer = new MediaPlayer(media);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return mediaPlayer;
    }

    private void updateMovablePieces() {
        matrixToStream(board.getTiles())
                .filter(tile -> !tile.isEmpty())
                .map(BlackTile::asBlackTile)
                .filter(tile -> tile.getPiece().getPieceColor().equals(activePlayer.getPieceColor()))
                .forEach(this::checkNeighborsAndSetMovable);
    }

    private void checkNeighborsAndSetMovable(BlackTile blackTile) {
        Piece piece = blackTile.getPiece();
        boolean movable = piece.getNeighborsThisPieceCanMoveTowards()
                .anyMatch(tile -> tile.isEmpty() || piece.canEatNeighbor(tile.getPiece()));
        piece.setMovable(movable);
    }

    public void reset() {
        board = new Board();
        log.clear();
        activePlayer = player1;
        newTurn();
        support.removePropertyChangeListener(drawer);
        drawer = drawer.reset();
        addPropertyChangeListener(drawer);
    }

    public Tile getActiveTile() {
        return activeTile;
    }

    public void setActiveTile(Tile tile) {
        this.activeTile = tile;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public Status getStatus() {
        return status;
    }

    public Board getBoard() {
        return board;
    }

    public void undo() {
        if (log.size() - 1 < 0) {
            throw new IllegalButtonClickException("An illegal click was performed on the undo button");
        }
        int[] coordinates = log.remove(log.size() - 1);
        move(coordinates[2], coordinates[3], coordinates[0], coordinates[1], false);
        drawer.updateBoard(board.getTiles());
        status = Status.IDLE;
    }

    public Graph generateGraphForTile(BlackTile source) {
        Graph graph = new Graph(board, source);
        Piece piece = source.getPiece();
        // Add edges for trivial moves (moves on empty squares, which weight 1)
        piece.getNeighborsThisPieceCanMoveTowards()
                .filter(Tile::isEmpty)
                .forEach(tile -> graph.addEdge(source, tile, 1));
        // Add edges for eating pieces
        piece.getNeighborsThisPieceCanMoveTowards()
                .filter(tile -> !tile.isEmpty() && piece.canEatNeighbor(tile.getPiece()))
                .forEach(tile -> graph.addEatingEdges(piece, tile.getPiece(), 1));
        return graph;
    }
}
