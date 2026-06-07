package utils;

/**
 * 坐标点类
 */
class CoordinatePoint {
    private String system;
    private double x;
    private double y;
    private double z;
    private String unit;
    
    public CoordinatePoint(String system, double x, double y, double z, String unit) {
        this.system = system;
        this.x = x;
        this.y = y;
        this.z = z;
        this.unit = unit;
    }
    
    public CoordinatePoint(String system, double x, double y, double z) {
        this(system, x, y, z, null);
    }
    
    public CoordinatePoint(String system, double x, double y) {
        this(system, x, y, 0, null);
    }
    
    public String getSystem() { return system; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getUnit() { return unit; }
    
    @Override
    public String toString() {
        if ("GEOGRAPHIC".equals(system)) {
            return String.format("地理坐标(经度: %.6f°, 纬度: %.6f°, 高度: %.2f)", x, y, z);
        } else {
            return String.format("%s(X: %.2f, Y: %.2f, Z: %.2f%s)", system, x, y, z, unit != null ? unit : "");
        }
    }
}

/**
 * 坐标转换器
 */
public class CoordinateTransformer {
    
    public static CoordinatePoint geographicToUTM(double longitude, double latitude, double altitude) {
        double easting = (longitude + 180) * 111320 * Math.cos(Math.toRadians(latitude));
        double northing = (latitude + 90) * 110540;
        return new CoordinatePoint("UTM", easting, northing, altitude, "m");
    }
    
    public static CoordinatePoint localToGeographic(double localX, double localY, double localZ,
        double originLon, double originLat, double originAlt) {
        double deltaLon = localX / (111320 * Math.cos(Math.toRadians(originLat)));
        double deltaLat = localY / 110540;
        double longitude = originLon + deltaLon;
        double latitude = originLat + deltaLat;
        double altitude = originAlt + localZ;
        return new CoordinatePoint("GEOGRAPHIC", longitude, latitude, altitude);
    }
    
    public static CoordinatePoint geographicToLocal(double longitude, double latitude, double altitude,
        double originLon, double originLat, double originAlt) {
        double localX = (longitude - originLon) * 111320 * Math.cos(Math.toRadians(originLat));
        double localY = (latitude - originLat) * 110540;
        double localZ = altitude - originAlt;
        return new CoordinatePoint("LOCAL", localX, localY, localZ, "m");
    }
    
    public static CoordinatePoint polarToCartesian(double r, double theta, double z) {
        double thetaRad = Math.toRadians(theta);
        double x = r * Math.cos(thetaRad);
        double y = r * Math.sin(thetaRad);
        return new CoordinatePoint("LOCAL", x, y, z, "m");
    }
    
    public static CoordinatePoint cartesianToPolar(double x, double y, double z) {
        double r = Math.sqrt(x * x + y * y);
        double theta = Math.toDegrees(Math.atan2(y, x));
        return new CoordinatePoint("POLAR", r, theta, z, "m");
    }
}
