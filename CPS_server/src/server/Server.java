package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
	/**
	 * main method - instantiates the server side
	 * @param args
	 */
	public static void main(String[] args) {
		ParkingStationController psc = new ParkingStationController();
		int[][][] arr = psc.getSlotStatus(1);
		//run subscriptions End Check every day
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new subscriptionsEndCheck(), 0, 1, TimeUnit.DAYS);
		scheduler.scheduleAtFixedRate(new lateOrderCheck(), 0, 15, TimeUnit.MINUTES);
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(8080);
			System.out.println("CPS server on ip: " + InetAddress.getLocalHost().getHostAddress().toString() +
					" port: " + String.valueOf(socket.getLocalPort()));

			while (true) {
				Socket currentSocket;
				currentSocket = socket.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));
				String s = br.readLine();
				System.out.println("[request] " + s);
				handelRequest(s, currentSocket);
			}
		} catch (Exception ioe) {
			System.out.println(ioe.getMessage());
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * handle incoming requests to the server
	 *
	 * @param request
	 * @param currentSocket
	 */
	private static void handelRequest(String request, Socket currentSocket) throws Exception {
		String cmd[] = ((String) request).split(" ");// arr cmd holds the 3 params of the possible commands:
		if (cmd.length < 2)// we want at least 2 params
		{
			System.out.println("too few arguments,at least 3 are needed");
		} else if (cmd[0].equals("login") && cmd[1].equals("client")) {
			CustomerController cc = new CustomerController();
			Customer c = cc.login(cmd[2], cmd[3]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			//PrintWriter pw = new PrintWriter(osw);
			osw.writeObject(c);
			osw.flush();
			currentSocket.close();

		} else if (cmd[0].equals("login") && cmd[1].equals("worker")) {
			WorkerController wc = new WorkerController();
			Worker w = wc.login(cmd[2], cmd[3], Worker.WorkerType.valueOf(cmd[4]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			//PrintWriter pw = new PrintWriter(osw);
			osw.writeObject(w);
			osw.flush();
			currentSocket.close();

		} else if (cmd[0].equals("get") && cmd[1].equals("stations")) {
			List<String> stations = ParkingStationController.getParkingIDs();
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(stations);
			osw.flush();
			currentSocket.close();
		} else if (cmd[0].equals("start") && cmd[1].equals("parking")) {
			boolean res = false;
			if(Car.getClientId(cmd[2]).equals(cmd[3])) {
				res = OrderController.startParking(cmd[2]);
			}
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} else if (cmd[0].equals("order") && cmd[1].equals("temporery")) {
			currentSocket.close();

		} else if (cmd[0].equals("add") && cmd[1].equals("client")) {
			CustomerController cc = new CustomerController();
			if (cmd.length < 9) {
				throw new Exception("not enough arguments for adding a new client");
			}
			boolean flag = cc.addNewClient(cmd[2], cmd[3], cmd[4], cmd[5], cmd[6], cmd[7], cmd[8]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			//PrintWriter pw = new PrintWriter(osw);
			osw.writeObject(true);
			osw.flush();
			currentSocket.close();

		} else if (cmd[0].equals("add") && cmd[1].equals("car")) {
			Car c = new Car(cmd[2], cmd[3]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(c.addNewCarToClient());
			osw.flush();
			currentSocket.close();
		} else if (cmd[0].equals("add") && cmd[1].equals("subscription")) {
			boolean res = OrderController.addNewSubscription(cmd[2], cmd[3], Timestamp.valueOf(cmd[4]), Timestamp.valueOf(cmd[5]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
			//orders
		} else if (cmd[0].equals("add") && cmd[1].equals("occasional")) {
			int res = OrderController.addOccasionalOrder(cmd[2], cmd[3] + " " + cmd[4], cmd[5], cmd[6]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();

		} else if (cmd[0].equals("add") && cmd[1].equals("advanced")) {
			int res = OrderController.addInAdvanceOrder(cmd[2], cmd[3] + " " + cmd[4], cmd[5] + " " + cmd[6], cmd[7], cmd[8]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		//================complaint handle============================================
		else if (cmd[0].equals("add") && cmd[1].equals("complaint"))
		{
			StringBuffer complaint_txt_builder = new StringBuffer();
			for (int i = 3; i < cmd.length; i++) {
				complaint_txt_builder.append( cmd[i] );
				complaint_txt_builder.append(" ");
			}
			String complaint_str = complaint_txt_builder.toString();
			
			boolean res = ComplaintController.addNewComplaint(cmd[2], complaint_str);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} 
		else if (cmd[0].equals("assign") && cmd[1].equals("attendant"))
		{
			String res = ComplaintController.assignAttendantToComlaint(cmd[2], cmd[3]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		else if (cmd[0].equals("respond") && cmd[1].equals("compalint"))
		{
			StringBuffer respond_txt_builder = new StringBuffer();
			for (int i = 3; i < cmd.length; i++) {
				respond_txt_builder.append( cmd[i] );
				respond_txt_builder.append(" ");
			}
			String respond_str = respond_txt_builder.toString();
			String res = ComplaintController.respondToCompalint(cmd[2], respond_str);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		else if (cmd[0].equals("add") && cmd[1].equals("compensation")) {
			String res = ComplaintController.addCompensationToCompalint(cmd[2], Float.valueOf(cmd[3]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		//=========end of complaints handling==================================
		else if (cmd[0].equals("end") && cmd[1].equals("parking")) {
			boolean res=false;
			if(Car.getClientId(cmd[2]).equals(cmd[3])) {
				int parkId = ParkingStationController.getParkId(OrderController.orderOngoingExist(cmd[2]));
				if(parkId != -1) {
//					ParkingStationController.removeCar(parkId, Integer.valueOf(cmd[2]));
					res = true;
				}
			}
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} else if (cmd[0].equals("get") && cmd[1].equals("endPrice")) {
			double res = OrderController.calcPriceOnEndOrder(cmd[2]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			OrderController.removeOrder(cmd[2], "ONGOING");
			currentSocket.close();
		} else if (cmd[0].equals("get") && cmd[1].equals("Price")) {
			double res = OrderController.getPriceById(Integer.valueOf(cmd[2]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} 
		
		
		else if (cmd[0].equals("cancel") && cmd[1].equals("order")) {
			double res = OrderController.cancelOrder(Integer.parseInt(cmd[2]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		else if (cmd[0].equals("get") && cmd[1].equals("OrderStatus")) {
			String res = OrderController.getOrderStatus(Integer.parseInt(cmd[2]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} 
		else if (cmd[0].equals("get") && cmd[1].equals("slots")) {
			int[][][] res = ParkingStationController.getSlotStatus(Integer.parseInt(cmd[2]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		} 
		//====================parking staion prices===========================================================
		else if (cmd[0].equals("ParkingStaion") && cmd[1].equals("updatePrices")) {
//			String res = ParkingStationController.setParkingPrices(cmd[2],cmd[3],Double.parseDouble(cmd[4]));
			boolean res = WorkerController.updatePrices(cmd[2], Double.valueOf(cmd[3]),Double.valueOf(cmd[4]),Double.valueOf(cmd[5]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		
		else if (cmd[0].equals("ParkingStaion") && cmd[1].equals("updatePricesCeoApprov")) {
			String res = ParkingStationController.updateParkingStaionPricesCeoApprove(cmd[2],cmd[3],cmd[4]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		//====================parking staion prices end===========================================================	
		else if (cmd[0].equals("logout") && cmd[1].equals("client")) {
			CustomerController.logout(cmd[2]);
		}
		else if (cmd[0].equals("reseved") && cmd[1].equals("slot")) {
			ParkingStationController.setResevedSlot(Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),Integer.parseInt(cmd[5]));
		}
		else if (cmd[0].equals("outOfOrder") && cmd[1].equals("slot")) {
			ParkingStationController.setOutOfOrderSlot(Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),Integer.parseInt(cmd[5]));
		}
		else if (cmd[0].equals("add") && cmd[1].equals("ParkingStaion")) {
			boolean res = ParkingStationController.addParkingStaion(cmd[2],Integer.parseInt(cmd[3]));
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}
		else if (cmd[0].equals("response") && cmd[1].equals("Message")) {
			OrderController.responseToMessage(Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]));	
		}
		else if (cmd[0].equals("Worker") && cmd[1].equals("Staion")) {
			String res = WorkerController.checkWorkerStaion(cmd[2]);
			ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
			osw.writeObject(res);
			osw.flush();
			currentSocket.close();
		}

	}
}
