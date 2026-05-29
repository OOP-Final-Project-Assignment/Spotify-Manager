package siu.k18.cntt.spotify.gui;

import siu.k18.cntt.spotify.dao.UserDAO;
import siu.k18.cntt.spotify.models.User;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private UserDAO userDAO;

    public LoginFrame() {
        // Initialize DAO object for database interaction
        this.userDAO = new UserDAO();

        // Configure basic window properties
        setTitle("Spotify Manager - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);
        setLayout(new BorderLayout());

        // --- UI DESIGN ---
        
        // Title Label
        JLabel lblTitle = new JLabel("SYSTEM LOGIN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(lblTitle, BorderLayout.NORTH);

        // Input Form Panel
        JPanel pnlForm = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlForm.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        pnlForm.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        pnlForm.add(txtUsername);

        pnlForm.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        pnlForm.add(txtPassword);

        add(pnlForm, BorderLayout.CENTER);

        // Buttons Panel
        JPanel pnlButtons = new JPanel(new FlowLayout());
        btnLogin = new JButton("Login");
        btnRegister = new JButton("Register Account");
        
        pnlButtons.add(btnLogin);
        pnlButtons.add(btnRegister);
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        
        add(pnlButtons, BorderLayout.SOUTH);

        // --- EVENT LISTENERS ---

        // 1. Login Button Event
        btnLogin.addActionListener(e -> performLogin());

        // Enable Enter key shortcut for instant login
        getRootPane().setDefaultButton(btnLogin);

        // 2. Register Button Event
        btnRegister.addActionListener(e -> {
            RegisterDialog dialog = new RegisterDialog(this, userDAO);
            dialog.setVisible(true);
        });
    }

    // Handles authentication logic using Modern Java (Optional)
    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Username and Password!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Authenticate from UserDAO (Returns Optional<User>)
        Optional<User> userOptional = userDAO.authenticate(username, password);
        
        userOptional.ifPresentOrElse(
            user -> {
                JOptionPane.showMessageDialog(this, "Login successful!\nWelcome: " + user.username() + " (" + user.role() + ")");
                this.dispose(); // Close login window
                new DashboardFrame(user).setVisible(true); // Open main dashboard
            },
            () -> {
                JOptionPane.showMessageDialog(this, "Invalid username or password!", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }
}

// =======================================================
// SUB-CLASS: REGISTRATION DIALOG WINDOW
// =======================================================
class RegisterDialog extends JDialog {
    private JTextField txtUsername;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JButton btnSubmit, btnCancel;

    public RegisterDialog(JFrame parent, UserDAO userDAO) {
        super(parent, "Register New Account", true);
        setSize(350, 250);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(4, 2, 10, 10));
        
        // Add padding to make the form look cleaner
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(new JLabel("Username:"));
        txtUsername = new JTextField();
        add(txtUsername);

        add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        add(txtPassword);

        add(new JLabel("Confirm Password:"));
        txtConfirmPassword = new JPasswordField();
        add(txtConfirmPassword);

        btnSubmit = new JButton("Sign Up");
        btnCancel = new JButton("Cancel");
        add(btnSubmit);
        add(btnCancel);

        btnCancel.addActionListener(e -> dispose());

        btnSubmit.addActionListener(e -> {
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword());
            String confirmPass = new String(txtConfirmPassword.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save user to Database (Defaults to 'User' role)
            boolean isSuccess = userDAO.registerUser(user, pass);
            
            if (isSuccess) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can log in now.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose(); 
            } else {
                JOptionPane.showMessageDialog(this, "Username already exists! Please choose another one.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}