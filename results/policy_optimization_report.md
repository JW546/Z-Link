# Policy Optimization Report

Optimization type: heuristic policy optimization.

Training status: heuristic policy optimization only; no RL / MARL training, no neural network training, no checkpoint.

## Summary

| Policy | Before reward | After reward | Before safetyRisk | After safetyRisk | Before conflictAccuracy | After conflictAccuracy | Reward up with safety risk up |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| WeightedUtilityAgentPolicy | 1.35 | 1.4 | 0.23 | 0.23 | 1.0 | 1.0 | false |
| AdaptiveAgentPolicy | 1.35 | 1.4 | 0.23 | 0.23 | 1.0 | 1.0 | false |

## WeightedUtilityAgentPolicy

- Optimization type: grid_search_plus_random_search
- Before weights: `{timeWeight=0.22, safetyWeight=0.34, congestionWeight=0.14, conflictWeight=0.12, confidenceWeight=0.1, freshnessWeight=0.08, replanningPenaltyWeight=0.08}`
- After weights: `{timeWeight=0.232, safetyWeight=0.559, congestionWeight=0.124, conflictWeight=0.103, confidenceWeight=0.087, freshnessWeight=0.067, replanningPenaltyWeight=0.234}`
- Best policy parameters: `{timeWeight=0.232, safetyWeight=0.559, congestionWeight=0.124, conflictWeight=0.103, confidenceWeight=0.087, freshnessWeight=0.067, replanningPenaltyWeight=0.234}`
- Improvement: `{averageRewardDelta=0.05, averageSafetyRiskDelta=0.0, averageTravelTimeDelta=-1.2, conflictResolutionAccuracyDelta=0.0}`
- Training status: `heuristic_parameter_search_no_rl_training`

## AdaptiveAgentPolicy

- Optimization type: heuristic_adaptive_update_seeded_by_best_weight_search
- Before weights: `{timeWeight=0.22, safetyWeight=0.34, congestionWeight=0.14, conflictWeight=0.12, confidenceWeight=0.1, freshnessWeight=0.08, replanningPenaltyWeight=0.08, learningRate=0.1, adaptive=true}`
- After weights: `{timeWeight=0.18249559774546828, safetyWeight=0.41087703821022736, congestionWeight=0.09114267037221499, conflictWeight=0.11972454949766233, confidenceWeight=0.0911965936105599, freshnessWeight=0.104563550563867, replanningPenaltyWeight=0.234, learningRate=0.1, adaptive=true}`
- Best policy parameters: `{timeWeight=0.18249559774546828, safetyWeight=0.41087703821022736, congestionWeight=0.09114267037221499, conflictWeight=0.11972454949766233, confidenceWeight=0.0911965936105599, freshnessWeight=0.104563550563867, replanningPenaltyWeight=0.234, learningRate=0.1, adaptive=true}`
- Improvement: `{averageRewardDelta=0.05, averageSafetyRiskDelta=0.0, conflictResolutionAccuracyDelta=0.0, averageTravelTimeDelta=-1.2}`
- Training status: `adaptive_heuristic_weight_update_no_rl_training`

## Safety Check

- Any reward improvement with safety-risk increase: false
- Interpretation: no reward improvement with safety-risk regression detected
