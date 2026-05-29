package siu.k18.cntt.spotify.main;

import siu.k18.cntt.spotify.gui.LoginFrame;
import siu.k18.cntt.spotify.dao.UserDAO;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
    	UserDAO userDAO = new UserDAO();
    	//userDAO.registerUser("admin", "123456", "Admin");
        // SwingUtilities.invokeLater giúp giao diện chạy mượt mà trên luồng EDT của Java
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
