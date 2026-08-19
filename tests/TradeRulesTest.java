import model.*;
import model.save.*;

public final class TradeRulesTest {
    public static void main(String[] args) {
        GameState state = new GameState(15, 13);
        HexCoordinate bazaarPosition = state.getTownHallPos().findNeighbours().get(0);
        Building bazaar = new Building(bazaarPosition, Constants.BuildingType.BAZAAR);
        state.getPlayer().addBuilding(bazaar);
        int food = state.getPlayer().getResources().get(Constants.ResourceType.FOOD);
        require(state.tradeAtBazaar(Constants.ResourceType.WOOD, Constants.ResourceType.FOOD, 10), "tier 10");
        require(state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == food + 5, "50 percent floor");
        require(!state.tradeAtBazaar(Constants.ResourceType.WOOD, Constants.ResourceType.FOOD, 10), "bazaar once per turn");

        HexCoordinate post = state.getMap().getTradingPosts().iterator().next();
        state.getPlayer().expandTerritory(post);
        require(state.tradeAtTradingPost(post, Constants.ResourceType.WOOD, Constants.ResourceType.STONE, 5),
                "post has independent turn usage");
        require(!state.tradeAtTradingPost(new HexCoordinate(-1, -1), Constants.ResourceType.WOOD,
                Constants.ResourceType.STONE, 5), "must be an actual post");
        System.out.println("TradeRulesTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
