package server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Server {
	public static void main(String[] args) {
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
				OrderController.startParking("21");
			}


		} catch (IOException ioe)
		{
			System.out.println(ioe.getMessage());
		} 
		finally
		{
			try {
				if (socket != null)
				{
					socket.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}
	/**
	 * handle incoming requests to the server
	 * @param request
	 * @param currentSocket
	 */
	private static void handelRequest(String request, Socket currentSocket) {
		String cmd[] = ((String) request).split(" ");// arr cmd holds the 3 params of the possible commands:
		if (cmd.length < 2)// we want at least 2 params
		{
			System.out.println("too few arguments,at least 3 are needed");
		} else if (cmd[0].equals("login") && cmd[1].equals("client")) {
			try {
				CustomerController cc = new CustomerController();
				Customer c = cc.login(cmd[2], cmd[3]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				//PrintWriter pw = new PrintWriter(osw);
				osw.writeObject(c);
				osw.flush();
				currentSocket.close();
			} catch (IOException ioe) {

			}
		} else if (cmd[0].equals("login") && cmd[1].equals("worker")) {
			try {
				WorkerController wc = new WorkerController();
				Worker w = wc.login(cmd[2], cmd[3], Worker.WorkerType.valueOf(cmd[4]));
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				//PrintWriter pw = new PrintWriter(osw);
				osw.writeObject(w);
				osw.flush();
				currentSocket.close();
			} catch (IOException ioe) {

			}
		} else if (cmd[0].equals("get") && cmd[1].equals("stations")) {
			try {
				List<String> stations = ParkingStationController.getParkingIDs();
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				osw.writeObject(stations);
				osw.flush();
				currentSocket.close();
			} catch (IOException ioe) {
			}
		} else if (cmd[0].equals("order") && cmd[1].equals("temporery")) {
			try {
				currentSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (cmd[0].equals("add") && cmd[1].equals("client")) {
			try {
				CustomerController cc = new CustomerController();
				if (cmd.length < 9) {
					throw new Exception("not enough arguments for adding a new client");
				}
				boolean flag = cc.addNewClient(cmd[2], cmd[3], cmd[4], cmd[5], cmd[6], cmd[7], cmd[8]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				//PrintWriter pw = new PrintWriter(osw);
				osw.writeObject(flag);
				osw.flush();
				currentSocket.close();
			} catch (Exception ioe) 
			{
				ioe.printStackTrace();
			}
		} else if (cmd[0].equals("add") && cmd[1].equals("car")) {
			try {
				Car c = new Car(cmd[2], cmd[3]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				osw.writeObject(c.addNewCarToClient());
				osw.flush();
				currentSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			//orders
		} else if (cmd[0].equals("add") && cmd[1].equals("occasional")) {
			try {
				boolean res = OrderController.addOccasionalOrder(cmd[2], cmd[3], cmd[4]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				osw.writeObject(res);
				osw.flush();
				currentSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (cmd[0].equals("add") && cmd[1].equals("advanced")) {
			try {
				boolean res = OrderController.addInAdvanceOrder(cmd[2], cmd[3], cmd[4]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				osw.writeObject(res);
				osw.flush();
				currentSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		//complaints:
		} else if (cmd[0].equals("add") && cmd[1].equals("complaint")) {
			try {
				boolean res = ComplaintController.addNewComplaint(cmd[2], cmd[3]);
				ObjectOutputStream osw = new ObjectOutputStream(currentSocket.getOutputStream());
				osw.writeObject(res);
				osw.flush();
				currentSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}


	}
}
