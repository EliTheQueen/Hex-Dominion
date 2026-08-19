package model.tribe;

import model.Constants;
import model.Player;
import model.trade.TradePolicy;
import model.trade.TradeService;

public class TribeTradeService implements java.io.Serializable {

    private final TradeService tradeService;

    private final TribeTradePolicyFactory
            policyFactory;

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

        TradePolicy policy =
                policyFactory.create(
                        tribe.getType()
                );

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
}
