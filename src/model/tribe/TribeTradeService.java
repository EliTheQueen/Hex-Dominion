package model.tribe;

import model.Constants;
import model.Player;
import model.trade.TradePolicy;
import model.trade.TradeService;

public class TribeTradeService implements java.io.Serializable {

    private final TradeService tradeService;

    private final TribeTradePolicyFactory
            policyFactory;
    private final java.util.Map<String, Integer> missionTradeBonusPercent = new java.util.HashMap<>();

    public TribeTradeService(
            TradeService tradeService,
            TribeTradePolicyFactory policyFactory
    ) {
        if (tradeService == null
                || policyFactory == null) {
            throw new IllegalArgumentException(
                    "dependencies must not be null"
            );
        }

        this.tradeService = tradeService;
        this.policyFactory = policyFactory;
    }

    public boolean trade(
            Player player,
            Tribe tribe,
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold,
            int currentTurn
    ) {
        if (player == null || tribe == null) {
            throw new IllegalArgumentException(
                    "player and tribe must not be null"
            );
        }

        if (!tribe.canTradeAt(currentTurn)) {
            return false;
        }

        TradePolicy policy = createPolicy(tribe);

        boolean completed =
                tradeService.complete(
                        player,
                        policy,
                        sell,
                        buy,
                        quantitySold
                );

        if (completed) {
            tribe.markTradedAt(currentTurn);
        }

        return completed;
    }

    public void activateMissionTradeBonus(Tribe tribe, int percent) {
        if (tribe == null || percent <= 0) throw new IllegalArgumentException("invalid mission trade bonus");
        missionTradeBonusPercent.merge(tribe.getId(), percent, Math::max);
    }

    public int getMissionTradeBonusPercent(Tribe tribe) {
        return tribe == null ? 0 : missionTradeBonusPercent.getOrDefault(tribe.getId(), 0);
    }

    public void clearTribeState(Tribe tribe) {
        if (tribe != null) missionTradeBonusPercent.remove(tribe.getId());
    }

    public boolean allowsTrade(Tribe tribe, Constants.ResourceType sell,
                               Constants.ResourceType buy) {
        if (tribe == null || sell == null || buy == null || tribe.getType() == TribeType.WARRIOR) return false;
        return createPolicy(tribe).allowsToTrade(sell, buy);
    }

    public int calculateReceiveAmount(Tribe tribe, int amount) {
        if (tribe == null || amount <= 0 || tribe.getType() == TribeType.WARRIOR) return 0;
        return createPolicy(tribe).calculateReceiveAmount(amount);
    }

    private TradePolicy createPolicy(Tribe tribe) {
        TradePolicy policy = policyFactory.create(tribe.getType());
        int bonus = missionTradeBonusPercent.getOrDefault(tribe.getId(), 0);
        return bonus > 0 ? new model.trade.TradeRateBonusPolicy(policy, bonus) : policy;
    }
}
