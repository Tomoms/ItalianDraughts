package it.units.italiandraughts.logic;

import it.units.italiandraughts.exception.IllegalButtonClickException;
import it.units.italiandraughts.ui.Drawer;
import it.units.italiandraughts.ui.PieceColor;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.jgrapht.GraphPath;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static it.units.italiandraughts.logic.StaticUtil.*;

public class Game {
    private Board board;
    private final Player player1;
    private final Player player2;
    private Player activePlayer;
    private Status status;
    private BlackTile activeTile;
    private Drawer drawer;
    private final PropertyChangeSupport support;
    private final List<Move> log;
    private final MediaPlayer mediaPlayer;
    private List<GraphPath<BlackTile, Edge>> absoluteLongestPaths;

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

        absoluteLongestPaths = graphs.stream()
                .flatMap(graph -> graph.getLongestPaths().stream())
                .collect(getLongestPaths());

        // TODO test print the cost of the absoluteLongestPaths and then the path, remove this two lines
        System.out.println(absoluteLongestPaths.get(0).getWeight());
        absoluteLongestPaths.forEach(System.out::println);
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

    public void movePiece(Piece piece, BlackTile destination) {
        BlackTile source = piece.getBlackTile();
        source.removePiece();
        destination.placePiece(piece);
    }

    private void playSound() {
        new Thread(() -> {
            mediaPlayer.play();
            mediaPlayer.seek(new Duration(0));
        }).start();
    }

    // TODO maybe shouldLog is useless
    public void moveStepByStep(Piece piece, List<BlackTile> steps, boolean shouldLog) {
        BlackTile source = piece.getBlackTile();
        List<EatenPiece> eatenPieces = new ArrayList<>();
        for (int i = 1; i < steps.size(); i++) {
            final BlackTile landingTile = steps.get(i);
            if (!piece.getReachableNeighboringBlackTiles().collect(Collectors.toList()).contains(landingTile)) {
                Optional<BlackTile> optionalOverTile = piece.getReachableNeighboringBlackTiles()
                        .filter(tile -> !tile.isEmpty() &&
                                landingTile.equals(piece.getPositionAfterEating(tile.getPiece())))
                        .findAny();
                if (optionalOverTile.isPresent()) {
                    EatenPiece eatenPiece = new EatenPiece(optionalOverTile.get());
                    eatenPieces.add(eatenPiece);
                    optionalOverTile.get().removePiece();
                }
            }
            movePiece(piece, landingTile);
        }

        if (shouldLog) {
            log.add(new Move(piece, source, steps.get(steps.size() - 1), eatenPieces));
        }

    }

    public void makeMove(Piece piece, List<BlackTile> steps, boolean shouldLog) {
        playSound();
        moveStepByStep(piece, steps, shouldLog);
        finalizeMove();
    }

    public void undoLastMove() {
        if (log.size() - 1 < 0) {
            throw new IllegalButtonClickException("An illegal click was performed on the undo button");
        }
        Move move = log.remove(log.size() - 1);
        move.getEatenPieces().forEach(EatenPiece::restore);
        Piece piece = move.getPiece();
        if (piece.getBlackTile().getY() == piece.getPieceColor().getPromotionRow()) {
            piece.setPieceType(PieceType.MAN);
        }
        movePiece(move.getPiece(), move.getSource());
    }

    private void finalizeMove() {
        drawer.updateBoard(board.getTiles());
        toggleActivePlayer();
        newTurn();
    }

    private MediaPlayer initMediaPlayer() {
        String path = "sounds" + File.separatorChar + "movePiece.mp3";
        URL resource = Objects.requireNonNull(getClass().getResource(path));
        Media media = new Media(resource.toString());
        return new MediaPlayer(media);
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
        boolean movable = piece.getReachableNeighboringBlackTiles()
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

    public void undo() {
        undoLastMove();
        finalizeMove();
        drawer.turnOffHighlightedSquares();
    }

    public Graph generateGraphForTile(BlackTile source) {
        Graph graph = new Graph(source, this);
        Piece piece = source.getPiece();
        // Add edges for trivial moves (moves on empty squares, which weight 1)
        piece.getReachableNeighboringBlackTiles()
                .filter(Tile::isEmpty)
                .forEach(tile -> graph.addEdge(source, tile, 1));
        // Add edges for eating pieces
        piece.getReachableNeighboringBlackTiles()
                .filter(tile -> !tile.isEmpty() && piece.canEatNeighbor(tile.getPiece()))
                .forEach(tile -> graph.recursivelyAddEatingEdges(piece, tile.getPiece(), 1));
        return graph;
    }

    public BlackTile getActiveTile() {
        return activeTile;
    }

    public void setActiveTile(BlackTile tile) {
        this.activeTile = tile;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public Board getBoard() {
        return board;
    }


    public Player getPlayer1() {
        return player1;
    }

    public List<GraphPath<BlackTile, Edge>> getAbsoluteLongestPaths() {
        return absoluteLongestPaths;
    }


    public Player getActivePlayer() {
        return activePlayer;
    }

    public List<Move> getLog() {
        return log;
    }

}
