/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.vehiclerouting.domain.timewindowed.solver;

import java.util.Objects;
import java.util.stream.Stream;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.examples.vehiclerouting.domain.Customer;
import org.optaplanner.examples.vehiclerouting.domain.Standstill;
import org.optaplanner.examples.vehiclerouting.domain.Vehicle;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedDepot;

// TODO When this class is added only for TimeWindowedCustomer, use TimeWindowedCustomer instead of Customer
public class ArrivalTimeUpdatingVariableListener implements VariableListener<VehicleRoutingSolution, Customer> {

    @Override
    public void beforeEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer) {
            updateArrivalTime(scoreDirector, (TimeWindowedCustomer) customer);
        }
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer) {
            updateArrivalTime(scoreDirector, (TimeWindowedCustomer) customer);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    protected void updateArrivalTime(ScoreDirector<VehicleRoutingSolution> scoreDirector,
                                     TimeWindowedCustomer sourceCustomer) {
        Standstill previousStandstill = sourceCustomer.getPreviousStandstill();
        Long departureTime = previousStandstill == null ? null
                : (previousStandstill instanceof TimeWindowedCustomer)
                ? ((TimeWindowedCustomer) previousStandstill).getDepartureTime()
                : ((TimeWindowedDepot) ((Vehicle) previousStandstill).getDepot()).getReadyTime();

        //Lovedeep Sangha Changes
        //here i am checking it has done 3 jobs or not then calculting the get work done hours then divid it by "100000"    because they have removed the decimal digit by 4 and 1 extra zero for only adding in  min  then getting hours then multiplying 15 min for every  hour
        Long restTime = checkDoneThreeJObsPreviouslyy(previousStandstill, 2) ? (getWorkDoneHours(previousStandstill) / 10000) * 15 : 0;
        int count = 0;
        TimeWindowedCustomer shadowCustomer = sourceCustomer;
        Long arrivalTime = calculateArrivalTime(shadowCustomer, departureTime, restTime);
        while (shadowCustomer != null && !Objects.equals(shadowCustomer.getArrivalTime(), arrivalTime)) {

            scoreDirector.beforeVariableChanged(shadowCustomer, "arrivalTime");
            shadowCustomer.setArrivalTime(arrivalTime);
            scoreDirector.afterVariableChanged(shadowCustomer, "arrivalTime");
            departureTime = shadowCustomer.getDepartureTime();
            if (count < 3) count++; // if the job is starting from here then coutn jobs till 3
            else {
//                then same as abouve  but check whether it is last customer of not if last then wait time is zero
                // then calculating the wait time as it is also act as work durationn
                restTime = shadowCustomer.getNextCustomer() == null ? checkDoneThreeJObsPreviouslyy(previousStandstill, 2) ? ((shadowCustomer.getDistanceFromPreviousStandstill() + departureTime) / 10000) * 15 : 0 : 0;
            }

            shadowCustomer = shadowCustomer.getNextCustomer();
            // calculating the ariival time via adding the waiting time
            arrivalTime = calculateArrivalTime(shadowCustomer, departureTime, restTime);

        }
    }
// get work done hours if they worked for 3 customer
    private Long getWorkDoneHours(Standstill previousStandstill) {
        while (previousStandstill != null) {
            return previousStandstill instanceof TimeWindowedCustomer ? ((TimeWindowedCustomer) previousStandstill).getDepartureTime() + getWorkDoneHours(previousStandstill) : (((TimeWindowedDepot) previousStandstill).getReadyTime() + getWorkDoneHours(previousStandstill));
        }
        return 0L;
    }

// check has done 3 ccustomer jobs previously
    private Boolean checkDoneThreeJObsPreviouslyy(Standstill previousStandstill, Integer n) {
        if (n == 0) {
            return previousStandstill instanceof TimeWindowedCustomer;
        } else {
            if (previousStandstill instanceof TimeWindowedCustomer) {
                checkDoneThreeJObsPreviouslyy((TimeWindowedCustomer) previousStandstill, n - 1);
            } else {
                return false;
            }
        }
        return false;
    }

    private Long calculateArrivalTime(TimeWindowedCustomer customer, Long previousDepartureTime, Long restTime) {
        if (customer == null || customer.getPreviousStandstill() == null) {
            return null;
        }
        if (customer.getPreviousStandstill() instanceof Vehicle) {
            // PreviousStandstill is the Vehicle, so we leave from the Depot at the best suitable time
//            and adding restTime time if available
            return Math.max(customer.getReadyTime() + restTime,
                    previousDepartureTime + restTime + customer.getDistanceFromPreviousStandstill());
        }
        return previousDepartureTime + customer.getDistanceFromPreviousStandstill();
    }

}
