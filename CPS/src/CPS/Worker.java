package CPS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import client.EchoServer;

public class Worker extends Person {

	public Worker(String id, String firstName, String lastName, String password, String type, String email, String telephone) {
		super(id, firstName, lastName, password, type, email, telephone);
	}


	public boolean setup() {
		return false;
	}

	public boolean reserveParkingSlot() {
		return false;
	}

	// TODO: return type is Unknown
	public int checkParkingAvailability() {
		return 0;
	}

	public boolean placeOrder() {
		return false;
	}

}
