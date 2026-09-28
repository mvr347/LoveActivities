package dev.lovelace.loveactivities.games.chess;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MiniChessGUI extends AbstractGUI {

    private final MiniChessGame game;
    private final boolean isWhite;
    private int selectedR = -1;
    private int selectedC = -1;
    private List<MiniChessBoard.Move> currentMoves = List.of();

    public MiniChessGUI(Player player, MiniChessGame game, boolean isWhite) {
        super(player, 54, "<gradient:#FF5E62:#FF9966>Мини-шахматы (6x7)</gradient>");
        this.game = game;
        this.isWhite = isWhite;
    }

    @Override
    public void initializeItems() {
        fillBorder();
        renderHeader();
        renderBoard();
        renderFooter();
    }

    private void fillBorder() {
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            setItem(i, glass);
        }
    }

    private void renderHeader() {
        Player p1 = Bukkit.getPlayer(game.getPlayer1());
        String p1Name = p1 != null ? p1.getName() : "Игрок 1";
        String p2Name = game.getP2Name();
        boolean isWhiteTurn = game.isWhiteTurn();
        String inactiveTex = LoveActivities.getInstance().getHeadManager().getTexture("ui.inactive_player");

        // P1 White (Slot 0)
        ItemBuilder p1Head;
        if (isWhiteTurn) {
            p1Head = new ItemBuilder(Material.PLAYER_HEAD)
                    .playerHead(game.getPlayer1())
                    .name("<white><bold>" + p1Name + " (Белые)</bold></white>")
                    .lore(
                            isWhite ? "<green><bold>▶ ВАШ ХОД!</bold></green>" : "<yellow>Ход белых фигур</yellow>",
                            "<gray>Играет белыми фигурами</gray>"
                    )
                    .glow(true);
        } else {
            p1Head = ItemBuilder.base64Head(inactiveTex)
                    .name("<gray><bold>" + p1Name + " (Белые - ожидает)</bold></gray>")
                    .lore(
                            "<dark_gray>Ожидает хода соперника...</dark_gray>",
                            "<gray>Играет белыми фигурами</gray>"
                    );
        }
        setItem(0, p1Head.build());

        // Status / Turn / Clock / Pot (Slot 9 on Left Flank)
        updateTimer();

        // P2 Black (Slot 8)
        ItemBuilder p2Head;
        if (!isWhiteTurn) {
            p2Head = new ItemBuilder(Material.PLAYER_HEAD)
                    .playerHead(game.getPlayer2())
                    .name("<dark_gray><bold>" + p2Name + " (Чёрные)</bold></dark_gray>")
                    .lore(
                            !isWhite ? "<green><bold>▶ ВАШ ХОД!</bold></green>" : "<yellow>Ход чёрных фигур</yellow>",
                            "<gray>Играет чёрными фигурами</gray>"
                    )
                    .glow(true);
        } else {
            p2Head = ItemBuilder.base64Head(inactiveTex)
                    .name("<gray><bold>" + p2Name + " (Чёрные - ожидает)</bold></gray>")
                    .lore(
                            "<dark_gray>Ожидает хода соперника...</dark_gray>",
                            "<gray>Играет чёрными фигурами</gray>"
                    );
        }
        setItem(8, p2Head.build());

        // Guide / Combinations (Slot 17)
        String tutHead = LoveActivities.getInstance().getHeadManager().getTexture("ui.tutorial");
        setItem(17, ItemBuilder.base64Head(tutHead)
                .name("<yellow><bold>[ОБУЧЕНИЕ]</bold></yellow>")
                .lore("<gray>Нажмите, чтобы прочитать правила Мини-шахмат.</gray>")
                .build());
    }

    private void renderBoard() {
        MiniChessBoard board = game.getBoard();

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                int slot = getSlotForTile(r, c);
                ChessPiece piece = board.getPiece(r, c);

                boolean isSelected = (r == selectedR && c == selectedC);
                boolean isValidMoveTarget = false;
                for (MiniChessBoard.Move m : currentMoves) {
                    if (m.toR() == r && m.toC() == c) {
                        isValidMoveTarget = true;
                        break;
                    }
                }

                if (isValidMoveTarget) {
                    if (piece != null) {
                        // Capture target head
                        String capTex = LoveActivities.getInstance().getHeadManager().getTexture("chess.tile_capture");
                        setItem(slot, ItemBuilder.base64Head(capTex)
                                .name("<red><bold>Захватить: " + piece.getType().getNameRu() + "</bold></red>")
                                .lore(
                                        "<gray>Позиция: <yellow>" + (char)('A' + c) + "" + (6 - r) + "</yellow></gray>",
                                        "<red>Нажмите, чтобы взять фигуру!</red>"
                                )
                                .glow(true)
                                .build());
                    } else {
                        // Move target head
                        String moveTex = LoveActivities.getInstance().getHeadManager().getTexture("chess.valid_move");
                        setItem(slot, ItemBuilder.base64Head(moveTex)
                                .name("<green><bold>Сюда (" + (char)('A' + c) + "" + (6 - r) + ")</bold></green>")
                                .lore(
                                        "<gray>Позиция: <yellow>" + (char)('A' + c) + "" + (6 - r) + "</yellow></gray>",
                                        "<green>Нажмите для перемещения</green>"
                                )
                                .glow(true)
                                .build());
                    }
                } else if (piece != null) {
                    String texKey = piece.getHeadTextureKey();
                    String b64 = LoveActivities.getInstance().getHeadManager().getTexture(texKey);

                    boolean isKingInCheck = (piece.getType() == ChessPiece.Type.KING && board.isKingInCheck(piece.getColor()));
                    String checkSuffix = isKingInCheck ? " <red><bold>[ШАХ!]</bold></red>" : "";

                    ItemBuilder builder = ItemBuilder.base64Head(b64)
                            .name(piece.getDisplayName() + checkSuffix + (isSelected ? " <yellow>[ВЫБРАНА]</yellow>" : ""))
                            .lore(
                                    "<gray>Позиция: <yellow>" + (char)('A' + c) + "" + (6 - r) + "</yellow></gray>",
                                    piece.getColor() == (isWhite ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK) ?
                                            "<green>▶ Ваша фигура (нажмите для хода)</green>" : "<red>Вражеская фигура</red>"
                                );

                    if (isSelected || isKingInCheck) {
                        builder.glow(true);
                    }

                    setItem(slot, builder.build());
                } else {
                    // Empty tile head (White tile / Black tile)
                    boolean isLight = (r + c) % 2 == 0;
                    String tileTex = LoveActivities.getInstance().getHeadManager().getTexture(isLight ? "chess.tile_white" : "chess.tile_black");
                    setItem(slot, ItemBuilder.base64Head(tileTex)
                            .name("<gray>" + (char)('A' + c) + "" + (6 - r) + "</gray>")
                            .lore(isLight ? "<white>Белая клетка</white>" : "<dark_gray>Чёрная клетка</dark_gray>")
                            .build());
                }
            }
        }
    }

    private void renderFooter() {
        String surrHead = LoveActivities.getInstance().getHeadManager().getTexture("ui.surrender");
        ItemStack surrItem = ItemBuilder.base64Head(surrHead)
                .name("<red><bold>[СДАТЬСЯ]</bold></red>")
                .lore("<gray>Признать поражение в матче.</gray>")
                .build();
        setItem(53, surrItem);

        String drawHead = LoveActivities.getInstance().getHeadManager().getTexture("ui.draw_offer");
        UUID offeredBy = game.getDrawOfferedBy();
        ItemBuilder drawBuilder = ItemBuilder.base64Head(drawHead);

        if (offeredBy == null) {
            drawBuilder.name("<yellow><bold>[ПРЕДЛОЖИТЬ НИЧЬЮ]</bold></yellow>")
                    .lore("<gray>Запросить ничью у соперника.</gray>");
        } else if (offeredBy.equals(player.getUniqueId())) {
            drawBuilder.name("<yellow><bold>[ОЖИДАНИЕ ОТВЕТА...]</bold></yellow>")
                    .lore("<gray>Вы предложили ничью.</gray>", "<gray>Ожидание решения соперника...</gray>")
                    .glow(true);
        } else {
            drawBuilder.name("<green><bold>[ПРИНЯТЬ НИЧЬЮ]</bold></green>")
                    .lore("<yellow>Соперник предложил ничью!</yellow>", "<green>▶ Нажмите, чтобы согласиться на ничью.</green>")
                    .glow(true);
        }

        setItem(44, drawBuilder.build());
    }

    public void updateTimer() {
        boolean isWhiteTurn = game.isWhiteTurn();
        boolean isTurn = isWhiteTurn == isWhite;
        String turnStr = isTurn ? "<green><bold>ВАШ ХОД</bold></green>" : "<yellow>ХОД СОПЕРНИКА</yellow>";
        String turnColor = isWhiteTurn ? "<white>Ход Белых</white>" : "<dark_gray>Ход Чёрных</dark_gray>";

        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, LoveActivities.getInstance().getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);
        String timerColor = remaining > 15 ? "<green>" : (remaining > 5 ? "<yellow>" : "<red><bold>");

        ChessPiece.Color myColor = isWhite ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        boolean myCheck = game.getBoard().isKingInCheck(myColor);
        boolean oppCheck = game.getBoard().isKingInCheck(myColor.opposite());

        List<Component> statusLore = new ArrayList<>();
        statusLore.add(TextUtil.parse("<gray>Текущий ход: " + turnColor + "</gray>"));
        statusLore.add(TextUtil.parse(turnStr));
        statusLore.add(TextUtil.parse("<gray>Осталось времени на ход: " + timerColor + remaining + " сек.</gray>"));
        statusLore.add(Component.empty());

        if (myCheck) {
            statusLore.add(TextUtil.parse("<red><bold>⚠ ВАШЕМУ КОРОЛЮ ОБЪЯВЛЕН ШАХ! ⚠</bold></red>"));
        } else if (oppCheck) {
            statusLore.add(TextUtil.parse("<yellow><bold>⚔ Вы объявили ШАХ королю соперника!</bold></yellow>"));
        } else {
            statusLore.add(TextUtil.parse("<gray>Цель: Поставить мат или захватить Короля!</gray>"));
        }

        String clockTex = LoveActivities.getInstance().getHeadManager().getTexture("ui.clock");
        String headerTitle = (game.getBet() > 0)
                ? "<gradient:#FF9966:#FF5E62><bold>Банк: " + (game.getBet() * 2) + " монет</bold></gradient> <dark_gray>•</dark_gray> " + timerColor + remaining + "с" + (remaining <= 5 ? " ⚠" : "")
                : "<gradient:#FF9966:#FF5E62><bold>Таймер хода</bold></gradient> <dark_gray>•</dark_gray> " + timerColor + remaining + "с" + (remaining <= 5 ? " ⚠" : "");

        setItem(9, ItemBuilder.base64Head(clockTex)
                .name(headerTitle)
                .lore(statusLore)
                .build());
    }

    private int getSlotForTile(int r, int c) {
        return r * 9 + 1 + c;
    }

    private int[] getTileFromSlot(int slot) {
        if (slot < 0 || slot >= 54) return null;
        int r = slot / 9;
        int c = (slot % 9) - 1;
        if (c >= 0 && c < 7 && r >= 0 && r < 6) {
            return new int[]{r, c};
        }
        return null;
    }

    @Override
    public void handleClick(int slot, ClickType clickType) {
        if (slot == 9) {
            SoundUtil.playClick(player);
            return;
        }
        if (slot == 17) {
            game.openTutorial(player);
            SoundUtil.playClick(player);
            return;
        }

        if (slot == 53) {
            game.resign(player);
            return;
        }

        if (slot == 44) {
            game.offerDraw(player);
            return;
        }

        if (game.isWhiteTurn() != isWhite) {
            SoundUtil.playError(player);
            return;
        }

        int[] tile = getTileFromSlot(slot);
        if (tile == null) return;

        int r = tile[0];
        int c = tile[1];

        // Check if clicking on an already highlighted valid move target
        for (MiniChessBoard.Move m : currentMoves) {
            if (m.toR() == r && m.toC() == c) {
                // Clear the selection BEFORE making the move: actionMakeMove() mutates the
                // board and re-renders synchronously (syncViews() -> initializeItems()), and
                // renderBoard() checks the still-stale `currentMoves` to decide whether a
                // square is a valid-move/capture target. Clearing after the call left the
                // destination square - now occupied by the piece that just moved there -
                // matching a leftover entry and rendering as "capture your own piece".
                selectedR = -1;
                selectedC = -1;
                currentMoves = List.of();
                game.actionMakeMove(player, m);
                return;
            }
        }

        // Otherwise, selecting own piece
        ChessPiece piece = game.getBoard().getPiece(r, c);
        ChessPiece.Color myColor = isWhite ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;

        if (piece != null && piece.getColor() == myColor) {
            selectedR = r;
            selectedC = c;
            currentMoves = game.getBoard().getLegalMoves(r, c);
            SoundUtil.playClick(player);
            initializeItems();
        } else {
            selectedR = -1;
            selectedC = -1;
            currentMoves = List.of();
            initializeItems();
        }
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
