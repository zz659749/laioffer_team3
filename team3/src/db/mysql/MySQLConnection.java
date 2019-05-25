package db.mysql;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.GeoLocation;
import org.json.JSONObject;

import database.DBConnection;
import entity.Address;
import entity.Order;
import entity.Robot;
import util.DistanceUtils;

//import util.GeoLocation;

public class MySQLConnection implements DBConnection{

	   private Connection conn;


	   public MySQLConnection() {
		    try {
		     Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
		     conn = DriverManager.getConnection(MySQLDBUtil.URL);
		     System.out.println("succeed connect to mysql local database");
		    } catch (Exception e) {
		     e.printStackTrace();
		    }
	   }
	   
	    public Connection getConnection() {
	    	return this.conn;
	    }


	    public JSONObject getOrderStatus(String orderId) {
	        JSONObject obj = new JSONObject();

	        try {
		        if (conn == null)
		            throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT * FROM orders WHERE order_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setString(1, orderId);

	            ResultSet rs = stmt.executeQuery();
               
	            while (rs.next()) {
	                String status = rs.getString("status");
	                Integer from_address_id = rs.getInt("from_address_id");
	                Integer to_address_id = rs.getInt("to_address_id");
	                Integer robot_id = rs.getInt("robot_id");
	                JSONObject fromAddress = getAddress(from_address_id);
	                JSONObject toAddress = getAddress(to_address_id);

	                obj.put("status", status);
	                obj.put("robot_id", robot_id);
	                obj.put("from_address", fromAddress);
	                obj.put("to_address", toAddress);
	                obj.put("expect_arrive_time", rs.getString("expect_arrive_time"));
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return obj;
	    }

	    public JSONObject getAddress(Integer addressId) {
//	    	System.out.println("connect database to get address by id : "  + addressId );
	        JSONObject obj = new JSONObject();

	        try {
		        if (conn == null)
		            throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT * FROM addresses WHERE address_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, addressId);

	            ResultSet rs = stmt.executeQuery();

	            while (rs.next()) {
	                obj.put("street", rs.getString("street"));
	                obj.put("city", rs.getString("city"));
	                obj.put("state", rs.getString("state"));
	                obj.put("zipcode", rs.getString("zipcode"));
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return obj;
	    }

	    public Integer getAddressId(Integer robotId) {
	        Integer addressId = null;

	        try {
		        if (conn == null)
		            throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT * FROM robots WHERE robot_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, robotId);

	            ResultSet rs = stmt.executeQuery();

	            while (rs.next()) {
	                addressId = rs.getInt("current_address_id");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return addressId;
	    }

	    public int getSpeed(Integer robotId){

	        int speed = 0;
	        try {
		        if (conn == null)
		            throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT * FROM robots WHERE robot_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, robotId);
	            ResultSet rs = stmt.executeQuery();
	            while (rs.next()) {
	                speed = rs.getInt("speed");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return speed;
	    }

	    // TODO: implement this method to create an address if not exists in database
	    public Integer createAddress(Address address) {

	        try {
	        	
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
		        
		    	System.out.println("craete address");
		        Integer addressId = null;
	            String sql = "INSERT IGNORE INTO addresses(street, city, state, zipcode) VALUES (?, ?, ?, ?)";
	            PreparedStatement stmt = conn.prepareStatement(sql);
//	            stmt.setNull(1,Types.INTEGER);
	            stmt.setString(1, address.getStreet());
	            stmt.setString(2, address.getCity());
	            stmt.setString(3, address.getState());
	            stmt.setString(4, address.getZipcode());
//	            System.out.println("final command is " + stmt.toString());
	            stmt.execute();
	            
	            sql = "SELECT LAST_INSERT_ID();";
	            stmt = conn.prepareStatement(sql);
	            ResultSet rs = stmt.executeQuery();
	            while (rs.next()) {
	                addressId = rs.getInt("LAST_INSERT_ID()");
	            }
	            System.out.println("the newest address_id is: " + addressId);
	            
	            return addressId;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return null;
	    }

	    // TODO: implement this method to get available (key: branch_id, value: branch_address) pairs
	    public JSONObject getBranchAddress(int branch_id) {

	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT address_id FROM branches WHERE branch_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, branch_id);
	            ResultSet rs = stmt.executeQuery();
	            while (rs.next()) {
	                 return  getAddress(rs.getInt("address_id"));
	            }
	            return null;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 

	        return null;
	    }
	    
	    public Map<Integer, Address> getAvailBranches() {
	    	Map<Integer, Address> map = new HashMap<>();

	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT * FROM branches";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            ResultSet rs = stmt.executeQuery();
	            
	            while (rs.next()) {
	                 Integer branchId = rs.getInt("branch_id");
	                 map.put(branchId, Address.parse(getAddress(rs.getInt("address_id"))));
	            }
	            return map;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 

	        return map;
	    }
	    
	    // implement this method to get all available robot with input type among all branches
	    public List<Integer> getAvailRobotIds(String robotType) {
	    	Integer avaRobotsInAllBranch = null;
	    	return getAvailRobotIds(robotType, avaRobotsInAllBranch);
	    }
	    // implement this method to get all available robot with input type in branch with branch_id
	    public List<Integer> getAvailRobotIds(String robotType, Integer branch_id) {
	    	List<Integer> robotIds = new ArrayList<Integer>();

	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	            String sql = "SELECT robot_id FROM robots WHERE status = ? AND type = ? ";
	            if(branch_id != null) {
	            	sql += " AND branch_id = ? ";
	            }
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            if(branch_id != null) {
	            	stmt.setInt(3, branch_id);
	            }
	            stmt.setString(1, Robot.IN_BRANCH);
	            stmt.setString(2, robotType);
	            ResultSet rs = stmt.executeQuery();
	            
	            while (rs.next()) {
	                 robotIds.add(rs.getInt("robot_id"));
	            }
	            return robotIds;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return null;
	    }

	    // TODO: implement this method to create a fake order
	    public boolean createOrder(Order order){

	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	            int fromAddressId = createAddress(order.getFromAddress());
	            int toAddressId = createAddress(order.getToAddress());
	            
	            String sql = "INSERT INTO orders(order_id, from_address_id, to_address_id, receiver_email, "
	            		+ "sender_email) VALUES(?, ?, ?, ?, ?);";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setString(1, order.getOrderId().toString());
	            stmt.setInt(2, fromAddressId);
	            stmt.setInt(3, toAddressId);
	            stmt.setString(4, order.getReceiverEmail());
	            stmt.setString(5, order.getSenderEmail());
	            stmt.execute();
	            return true;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return false;
	    }

	    // TODO: implement this method to complement robot information(type, branch_id, geolocation) with input robot. 
	    public boolean complementRobot(Robot robot) {
	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	            int robot_id = robot.getRobotId();
	            String sql = "SELECT * from robots where robot_id = ?;";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, robot_id);
	            ResultSet rs = stmt.executeQuery();
	            while (rs.next()) {
	            	System.out.println("complementing robot...");
	            	robot.setType(rs.getString("type"));
	            	if(robot.getType().equals(Robot.UAV)) {
	            		robot.setSpeed(rs.getInt("speed"));
	            	}
	            	robot.setBranchAddress(Address.parse(getBranchAddress(rs.getInt("branch_id"))));
	            	GeoLocation location = DistanceUtils.getGeocode(robot.getBranchAddress());
	                robot.setCurrentGeoLocation(location);
	                return true;
	            }
	            return false;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return false;
	    }
	    
	    public boolean deleteOrder(String orderId) {
	    	
	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	            String sql = "DELETE FROM orders where order_id = ?";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setString(1, orderId);
	            return stmt.execute();
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return false;
	    }
	    
	    
	    //TODO 将临时改为确认
	    public boolean confirmOrder(JSONObject input) {

	        try {
	        	if (conn == null)
		    		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	        	JSONObject robotJsonObject = input.getJSONArray("robot").getJSONObject(0);
	            String sql = "UPDATE orders SET price = ?, appointment_time = ?, status = ?, expect_arrive_time = ? WHERE order_id = ?;";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setDouble(1, robotJsonObject.getDouble("price"));
	            stmt.setString(2, robotJsonObject.getString("appointment_time"));
	            stmt.setString(3, Order.STATUS_INITIAL);
	            //update epect arrive time
	            String time = robotJsonObject.getString("appointment_time");
	            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            Date date =sdf.parse(time);
	            Calendar arriveTime = Calendar.getInstance();
	            arriveTime.setTime(date);
	            arriveTime.add(Calendar.MINUTE, robotJsonObject.getInt("time"));
	            stmt.setString(4,  sdf.format(arriveTime.getTime()));
	            stmt.setString(5, input.getString("order_id"));
	            return stmt.execute();
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return false;
	    }
	    // TODO 当有机器人开始执行这项order时，更新order信息
	    public boolean updateOrder(String orderId, Integer robotId) {

	        try {
	        	if (conn == null)
		    		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	            String sql = "UPDATE orders SET robot_id = ? WHERE order_id = ?;";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, robotId);
	            stmt.setString(2, orderId);
	            return stmt.execute();
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return false;
	    }
	    
	    public void close() {
		    if (conn != null) {
		    	try {
			      this.conn.close();
			     } catch (Exception e) {
			      e.printStackTrace();
			     }
			    }
	    }

		@Override
		public JSONObject getRobotInformation(Integer robot_id) {
	        try {
	        	if (conn == null)
	        		throw new FileNotFoundException("No connection to database");
	        	// create from_address and to_address first;
	            String sql = "SELECT * from robots where robot_id = ?;";
	            PreparedStatement stmt = conn.prepareStatement(sql);
	            stmt.setInt(1, robot_id);
	            ResultSet rs = stmt.executeQuery();
	            while (rs.next()) {
	            	JSONObject robot = new JSONObject();
	            	robot.put("robot_id", rs.getInt("robot_id"));
	            	robot.put("branch_id", rs.getInt("branch_id"));
	            	robot.put("type", rs.getString("type"));
	            	//robot.put("max_load", rs.getInt("max_load"));
	            	robot.put("speed", rs.getInt("speed"));
	            	robot.put("endurance", rs.getInt("endurance"));
	            	robot.put("status", rs.getString("status"));
	            	robot.put("current_order_id", rs.getString("current_order_id"));
	            	robot.put("current_lat", rs.getDouble("current_lat"));
	            	robot.put("current_lng", rs.getDouble("current_lng"));
	                return robot;
	            }
	            return null;
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        } 
	        return null;
		}

}