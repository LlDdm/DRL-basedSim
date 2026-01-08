/*
 * Title:        EdgeCloudSim - Scenario Factory
 *
 * Description:  Sample scenario factory providing the default
 *               instances of required abstract classes
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package com.lilin;

public class LScenarioFactory implements ScenarioFactory {
    public LScenarioFactory() {
    }

    @Override
    public LoadGeneratorModel getLoadGeneratorModel() {
        return new LoadGeneratorModel(SimSettings.getLOAD_GENERATE_TIME(), SimSettings.getWarmUpPeriod());
    }

    @Override
    public EdgeDeviceGeneratorModel getDeviceGeneratorModel() {
        return new EdgeDeviceGeneratorModel();
    }

    @Override
    public NetWork getNetworkModel() {
        return new NetWork();
    }

    @Override
    public Scheduler getScheduler() {
        return new Scheduler();
    }
}


