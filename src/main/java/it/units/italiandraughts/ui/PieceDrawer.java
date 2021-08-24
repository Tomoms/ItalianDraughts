package it.units.italiandraughts.ui;

import it.units.italiandraughts.logic.Piece;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class PieceDrawer{


    private final Ellipse baseEllipse;
    private final Ellipse upperEllipse;
    private final Piece piece;
    private final Tile tile;


    public PieceDrawer(Piece piece, Tile tile) {
        this.piece = piece;
        this.tile = tile;

        baseEllipse = createEllipse(tile.getSize());
        baseEllipse.setFill(Color.BLACK);
        baseEllipse.setTranslateY(tile.getSize() * 0.07);

        upperEllipse = createEllipse(tile.getSize());
        upperEllipse.setFill(Color.valueOf(piece.getPieceType().getHexColor()));
    }

    private Ellipse createEllipse(double tileSize) {
        Ellipse ellipse = new Ellipse(tileSize * 0.3125, tileSize * 0.26);
        ellipse.setStroke(Color.BLACK);
        ellipse.setStrokeWidth(tileSize * 0.03);
        return ellipse;
    }

    public Piece getPiece() {
        return piece;
    }

    void draw() {
        tile.getChildren().add(baseEllipse);
        tile.getChildren().add(upperEllipse);
    }

    public Ellipse getBaseEllipse() {
        return baseEllipse;
    }

    public Ellipse getUpperEllipse() {
        return upperEllipse;
    }
}