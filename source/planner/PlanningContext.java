package planner;

public class PlanningContext {
    public boolean passengerSafetyFirst;
    public boolean passengerUrgent;
    public boolean emergencyOverrideAllowed;
    public double safetyRiskThreshold = 0.70;
    public double passengerPriority = 0.70;

    public static PlanningContext safetyFirst() {
        PlanningContext context = new PlanningContext();
        context.passengerSafetyFirst = true;
        context.passengerUrgent = true;
        context.passengerPriority = 0.88;
        return context;
    }

    public static PlanningContext fastest() {
        PlanningContext context = new PlanningContext();
        context.passengerUrgent = true;
        context.passengerPriority = 0.78;
        return context;
    }
}
