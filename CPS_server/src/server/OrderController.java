package server;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderController {
	ParkingStationController psc = new ParkingStationController();

	public enum OrderStatus {
		PENDING, ONGOING;
	}

	public enum OrderType {
		//todo edit more statuses
		OCCASIONAL, IN_ADVANCE, SUBSCRIBED;
	}

	public enum PaymentMethod {
		CREDIT, CASH;
	}

	private static server.sqlConnection sql = server.sqlConnection.getInstant();


	/**
	 * finds the status of the order by give order id
	 *
	 * @param order_id
	 * @return the status of current order
	 */
	public static String getOrderStatus(int order_id) {
		java.sql.PreparedStatement stmt = null;

		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_id = ?");

			stmt.setInt(1, order_id);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				return null;
			}
			return rs.getString("order_status");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * adds new subscription to db
	 *
	 * @param cliendID
	 * @param carID
	 * @param startDate
	 * @param endDate
	 * @return result string
	 */
	public static boolean addNewSubscription(String cliendID, String carID, java.sql.Timestamp startDate,
											java.sql.Timestamp endDate) {
		Statement stmt;
		boolean res=false;
		try {
			stmt = sql.conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

			ResultSet c = stmt.executeQuery("SELECT * FROM clients WHERE client_ID=" + cliendID + ";");
			if (!c.next()) {
				System.err.println("no client with such id");
				return res;
			}
			ResultSet client = stmt.executeQuery("SELECT * FROM subscriptions WHERE client_ID=" + cliendID + ";");
			if (!client.next()) {
				ResultSet uprs = stmt.executeQuery("SELECT * FROM subscriptions");
				uprs.moveToInsertRow();
				uprs.updateString("client_ID", cliendID);
				uprs.updateString("car_ID", carID);
				uprs.updateTimestamp("start_date", startDate);
				uprs.updateTimestamp("end_date", endDate);

				uprs.insertRow();

				System.out.println("New subscription was added succsfully");
				res=true;
				uprs.close();
				stmt.close();
			} 
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return res;
	}

	/**
	 * adds an order for occasional clients
	 * the method adds a client, a car and an order if any does not exist already
	 *
	 * @param carID
	 * @param endDate
	 * @param parkingName
	 * @param paymentMethod
	 * @return true on successful add. false otherwise
	 */
	public static int addOccasionalOrder(String carID, String endDate, String parkingName, String paymentMethod) {
		int id = -1;
		PreparedStatement stmt;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_car_id=?");
			stmt.setString(1, carID);
			ResultSet client = stmt.executeQuery();
			if (!client.next() || (client.next() && !OrderOverlaps(carID, client.getString(7), endDate, parkingName))) {
				if (ParkingStationController.insertCar(getOrderParkingId(parkingName), LocalDateTime.parse(endDate, formatter), carID) == true) {

					stmt = sql.conn.prepareStatement("INSERT INTO orders (order_status,order_car_id,order_type,end_date,order_parking_id,order_payment_method) VALUES (?,?,?,?,?,?)"
							, Statement.RETURN_GENERATED_KEYS);

					stmt.setString(1, OrderStatus.PENDING.toString());
					stmt.setString(2, carID);
					stmt.setString(3, OrderType.OCCASIONAL.toString());
					stmt.setString(4, endDate);
					stmt.setInt(5, getOrderParkingId(parkingName));
					stmt.setString(6, paymentMethod);
					stmt.executeUpdate();

					ResultSet uprs = stmt.getGeneratedKeys();
					if (uprs.next()) {
						id = uprs.getInt(1);
						calcAndUpdatePrice(id);
					}

					System.out.println("New order was added successfully");

					uprs.close();
					stmt.close();
				} else {
					System.out.println("order already exists for current car");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * finds parking id by its name
	 *
	 * @param parkingName
	 * @return parking id or -1 if was not found.
	 * it may be not possible to return the value -1 since the name is selected from existing names in db
	 * but the value is initiated for testing purposes
	 */
	public static int getOrderParkingId(String parkingName) {
		int res = -1;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT parking_id FROM ParkingStation WHERE parking_address=?;");
			stmt.setString(1, parkingName);
			ResultSet client = stmt.executeQuery();
			if (client.next()) {
				return client.getInt(1);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	/**
	 * adds an order for clients that wish to reserve a parking slot
	 * the method adds a client, a car and an order if any does not exist already
	 *
	 * @param carID
	 * @param startDate
	 * @param endDate
	 * @param parkingName
	 * @param paymentMethod
	 * @return true if add was successful. false otherwise
	 */
	public static int addInAdvanceOrder(String carID, String startDate, String endDate, String parkingName, String paymentMethod) {
		int id = -1;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_car_id=?");
			stmt.setString(1, carID);
			ResultSet client = stmt.executeQuery();
			if ((!OrderOverlaps(carID, startDate, endDate, parkingName)) && (ParkingStationController.checkAvilablePlace(getOrderParkingId(parkingName), startDate, endDate))) {

				stmt = sql.conn.prepareStatement("INSERT INTO orders (order_status,order_car_id,order_type,start_date,end_date,order_parking_id,order_payment_method ) VALUES (?,?,?,?,?,?,?)"
						, Statement.RETURN_GENERATED_KEYS);

				stmt.setString(1, OrderStatus.PENDING.toString());
				stmt.setString(2, carID);
				stmt.setString(3, OrderType.IN_ADVANCE.toString());
				stmt.setString(4, startDate);
				stmt.setString(5, endDate);
				stmt.setInt(6, getOrderParkingId(parkingName));
				stmt.setString(7, paymentMethod);
				stmt.executeUpdate();

				ResultSet uprs = stmt.getGeneratedKeys();
				if (uprs.next()) {
					id = uprs.getInt(1);
					calcAndUpdatePrice(id);
				}

				System.out.println("New order was added successfully");
				uprs.close();
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * check if there is already an order for current car that overlaps new order
	 *
	 * @param carID       of optionally future order
	 * @param startDate   of optionally future order.
	 * @param endDate     of optionally future order
	 * @param parkingName of optionally future order
	 * @return true if found overlapping order in same parking station
	 */
	public static boolean OrderOverlaps(String carID, String startDate, String endDate, String parkingName) {
//		String _startDate = startDate != null ? startDate : (new Timestamp(System.currentTimeMillis())).toString();
		java.sql.PreparedStatement stmt;
		try {

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			java.util.Date start = dateFormat.parse(startDate);
			Timestamp start_date_timestamp = new java.sql.Timestamp(start.getTime());

			dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			java.util.Date end = dateFormat.parse(endDate);
			Timestamp end_date_timestamp = new java.sql.Timestamp(end.getTime());

			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_car_id = ?");
			stmt.setString(1, carID);


			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
//				int numColumns = rs.getMetaData().getColumnCount();
//				for (int i = 1; i <= numColumns; i++) {
				// Column numbers start at 1.
				// Also there are many methods on the result set to return
				//  the column as a particular type. Refer to the Sun documentation
				//  for the list of valid conversions.

				Timestamp order_start_time = rs.getTimestamp(7);
				Timestamp order_end_time = rs.getTimestamp(8);
				if (parkingName.equals(ParkingStationController.getParkingNameByID(rs.getInt(6)))) {

					if (start_date_timestamp.after(order_start_time) && start_date_timestamp.before(order_end_time)) {
						System.err.println("order failed. start time overlaps existing order");
						return true;
					}
					if (end_date_timestamp.after(order_start_time) && end_date_timestamp.before(order_end_time)) {
						System.err.println("order failed. end time overlaps existing order");
						return true;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * check for Available Order of given car in current time
	 *
	 * @param parkId
	 * @param carId
	 * @return true if there is order
	 */
	public boolean checkAvailableOrder(int parkId, int carId) {
		java.sql.PreparedStatement stmt = null;
		int numOfOrders = 0;
		try {
			stmt = sql.conn.prepareStatement("SELECT count(*) FROM orders WHERE order_parking_id = ? AND order_car_id	= ? AND start_date <= NOW() AND end_date > NOW()");
			stmt.setInt(1, parkId);
			stmt.setInt(2, carId);


			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				numOfOrders = rs.getInt(1);
			}

			if (numOfOrders > 0) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * method allows client to park his car.
	 * changes order status to ongoing on success
	 *
	 * @param carID
	 * @return true if order is in status pending and the current time matches order start time. false otherwise
	 */
	public static boolean startParking(String carID) {
		boolean flag = false;
		int res = 0;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_car_id = ? AND order_status = ?");
			stmt.setString(1, carID);
			stmt.setString(2, OrderStatus.PENDING.toString());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int orderID = rs.getInt(1);
				Timestamp startTime = rs.getTimestamp(7);
				Timestamp endTime = rs.getTimestamp(8);
				Timestamp nowTime = new Timestamp(System.currentTimeMillis());
				if (getTimeDiffInHours(startTime, nowTime) >= 0 && getTimeDiffInHours(nowTime, endTime) > 0) {

					stmt = sql.conn.prepareStatement("UPDATE  `Group_1`.`orders` SET  `order_status` =  ? WHERE  `orders`.`order_id` =?;");
					stmt.setString(1, OrderStatus.ONGOING.toString());
					stmt.setInt(2, orderID);
					res = stmt.executeUpdate();
				}
			}
			if (res == 1) {
				System.out.println("parking started succsfully");
				flag = true;
			} else {
				System.err.println("cannot start parking");
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * checks if the order is ongoing
	 *
	 * @param carID of order
	 * @return park station id of current car
	 */
	public static int orderOngoingExist(String carID) {
		int id = -1;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM `orders` WHERE `order_car_id`=\"" + carID + "\" AND `order_status`=\"ONGOING\"");
			//			stmt.setString(1, carID);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				id = rs.getInt(1);
				System.out.println("order ongoing exists");
			} else {
				System.out.println("order ongoing doesnt exist");
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * checks if the order is pending
	 *
	 * @param carID
	 * @return true if exists
	 */
	public static boolean orderPendingExist(String carID) {
		boolean flag = false;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM `orders` WHERE `order_car_id`=\"" + carID + "\" AND `order_status`=\"PENDING\"");
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				flag = true;
				System.out.println("order pending exists");
			} else {
				System.out.println("order pending doesnt exist");
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * @param startTime
	 * @param endTime
	 * @return difference between two timestamps in hours
	 */
	private static double getTimeDiffInHours(Timestamp startTime, Timestamp endTime) {
		long diff = endTime.getTime() - startTime.getTime();
		return diff / 3600000.0;
	}

	/**
	 * @param parking_id
	 * @param type
	 * @return price per hour of given parking station and order type (subscription, occasional, one-time)
	 */
	public static double getPricePerHour(int parking_id, String type) {
		double price_per_hour = Double.MAX_VALUE;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT order_price_per_hour FROM order_prices WHERE parking_id = ? AND order_type = ?");
			stmt.setInt(1, parking_id);
			stmt.setString(2, type);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				price_per_hour = rs.getDouble(1);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return price_per_hour;
	}
	
	/**
	 * @param odrer_id
	 * @return get order price of a specific order
	 */
	public static double getPriceById(int odrer_id) {
		double price = Double.MAX_VALUE;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT order_price FROM orders WHERE `orders`.`order_id` = ?");
			stmt.setInt(1, odrer_id);
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				price = rs.getDouble(1);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return price;
	}
	
	/**
	 * calculates payment balance when a client wishes to finish parking
	 *
	 * @param carID
	 * @return payment balance (may be negative - means client gets refund)
	 */
	public static double calcPriceOnEndOrder(String carID) {
		double res = Double.MAX_VALUE, price_per_hour = 0, hours = 0;

		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_car_id = ?");
			stmt.setString(1, carID);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				Timestamp startTime = rs.getTimestamp("start_date");
				Timestamp endTime = rs.getTimestamp("end_date");
				Timestamp nowTime = new Timestamp(System.currentTimeMillis());
				hours = getTimeDiffInHours(startTime, nowTime) - getTimeDiffInHours(startTime, endTime);

				String type = rs.getString(5);
				int parking_id = rs.getInt(6);

				price_per_hour = getPricePerHour(parking_id, type);
				if (price_per_hour != Double.MAX_VALUE) {

					res = hours * price_per_hour;
					System.out.println("price calculated succsfully");
				}
			} else {
				System.out.println("price calculation failed");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * evaluates and updates price of given order (depending on current time)
	 *
	 * @param order_id
	 */
	public static void calcAndUpdatePrice(int order_id) {
		double res = Double.MAX_VALUE, price_per_hour = 0, hours = 0;
		PreparedStatement stmt;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_id = ?");
			stmt.setInt(1, order_id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				Timestamp startTime = rs.getTimestamp("start_date");
				Timestamp endTime = rs.getTimestamp("end_date");
				hours = getTimeDiffInHours(startTime, endTime);

				String type = rs.getString(5);
				int parking_id = rs.getInt(6);
				price_per_hour = getPricePerHour(parking_id, type);

				if (price_per_hour != Double.MAX_VALUE) {
					res = hours * price_per_hour;
					stmt = sql.conn.prepareStatement("UPDATE orders SET order_price = ? WHERE order_id = ?");
					stmt.setDouble(1, res);
					stmt.setInt(2, order_id);
					stmt.executeUpdate();

					System.out.println("price " + res + "calculated succsfully");
				}
			} else {
				System.out.println("price calculation failed");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * removes order of current order
	 *
	 * @param order_id
	 * @return true if remove succeeded. false otherwise
	 */
	public static boolean removeOrderById(int order_id) {
		java.sql.PreparedStatement stmt;
		boolean return_res = false;
		try {
			stmt = sql.conn.prepareStatement("DELETE FROM orders WHERE order_id = ?");
			stmt.setInt(1, order_id);

			if (stmt.executeUpdate() == 1) {
				System.out.println("order deleted successfully");
				return_res = true;
			}
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return return_res;
	}

	// type ONGOING for end parking, PENDING for cancel

	/**
	 * removes order of current order
	 * type ONGOING for end parking, PENDING for cancel
	 *
	 * @param carID
	 * @param type
	 * @return true if remove succeeded. false otherwise
	 */
	public static boolean removeOrder(String carID, String type) {
		boolean res = false;
		java.sql.PreparedStatement stmt;
		String clientId = Car.getClientId(carID);
		if (Car.getClientCarsById(clientId).size() == 1) {
			if (CustomerController.removeCustomer(clientId)) {
				res = true;
			}
		} else {
			try {
				stmt = sql.conn.prepareStatement("DELETE FROM orders WHERE order_car_id = ? AND order_status = ?");
				stmt.setString(1, carID);
				stmt.setString(2, type);
				if (stmt.executeUpdate() == 1) {
					res = true;
				}
				stmt.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (res)
			System.out.println("order removed successfully");
		else
			System.out.println("remove order failed");

		return res;
	}

	/**
	 * cancels current order
	 *
	 * @param orderId
	 * @return payment balance (depends when the client makes the cancellation)
	 */
	public static double cancelOrder(int orderId) {
		PreparedStatement stmt;
		double diff = 0, price = Double.MAX_VALUE;
		try {
			stmt = sql.conn.prepareStatement("SELECT * FROM orders WHERE order_id = ?");
			stmt.setInt(1, orderId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				Timestamp startTime = rs.getTimestamp("start_date");
				Timestamp nowTime = new Timestamp(System.currentTimeMillis());
				diff = getTimeDiffInHours(startTime, nowTime);
				price = rs.getDouble(9);

				if (diff <= 1)
					;
				else if (diff < 3 && diff > 1)
					price *= 0.5;
				else
					price *= 0.9;

				removeOrderById(orderId);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return price;
	}

	/**
	 * updates response to current message
	 *
	 * @param response
	 * @param messages_id
	 */
	public static void responseToMessage(int response, int messages_id) {
		java.sql.PreparedStatement stmt = null;
		try {
			stmt = sql.conn.prepareStatement("UPDATE Messages SET messages_confirmation = ? WHERE messages_id = ?");
			stmt.setInt(1, response);
			stmt.setInt(2, messages_id);

			int rs = stmt.executeUpdate();

		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				/* ignored */
			}
		}

	}
}
