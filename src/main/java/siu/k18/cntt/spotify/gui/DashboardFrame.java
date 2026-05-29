package siu.k18.cntt.spotify.gui;

import siu.k18.cntt.spotify.dao.TrackDAO;
import siu.k18.cntt.spotify.dao.ArtistDAO;
import siu.k18.cntt.spotify.dao.UserDAO;
import siu.k18.cntt.spotify.models.Track;
import siu.k18.cntt.spotify.models.Artist;
import siu.k18.cntt.spotify.models.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

// Java GUI Imports
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

// Apache POI Imports (Excel Export)
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class DashboardFrame extends JFrame {
    private User loggedInUser;
    
    // --- Variables for Track Tab ---
    private JTable tblTracks;
    private DefaultTableModel trackTableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private TrackDAO trackDAO;
    private int currentPage = 0;
    private final int PAGE_SIZE = 100;
    private JLabel lblPage, lblTotalTracks;
    private String currentSearchCol = "All";
    private String currentKeyword = "";

    // --- Variables for Artist Tab ---
    private JTable tblArtists;
    private DefaultTableModel artistTableModel;
    private ArtistDAO artistDAO;
    private int currentArtistPage = 0;
    private JLabel lblArtistPage, lblTotalArtists;
    private String currentArtistKeyword = "";

    public DashboardFrame(User user) {
        this.loggedInUser = user;
        this.trackDAO = new TrackDAO();
        this.artistDAO = new ArtistDAO();
        
        setTitle("Spotify Manager - Current User: " + user.username() + " (" + user.role() + ")");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ==========================================
        // HEADER: INFO, THEME SWITCHER AND LOGOUT
        // ==========================================
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); 
        pnlHeader.setBackground(new Color(240, 240, 240)); 
        
        JLabel lblWelcome = new JLabel("Welcome, " + user.username() + " | Role: " + user.role());
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 14));
        
        // --- MODERN JAVA: var and Enhanced Switch ---
        var roleName = user.role();
        Color roleColor = switch (roleName.toLowerCase()) {
            case "admin" -> new Color(220, 53, 69); 
            case "staff" -> new Color(0, 123, 255); 
            default -> new Color(40, 167, 69);      
        };
        lblWelcome.setForeground(roleColor);

        pnlHeader.add(lblWelcome, BorderLayout.WEST);

        JPanel pnlHeaderRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlHeaderRight.setOpaque(false); 

        JButton btnTheme = new JButton("🎨 Change Theme");
        btnTheme.setFocusPainted(false);
        pnlHeaderRight.add(btnTheme);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(220, 53, 69)); 
        btnLogout.setForeground(Color.WHITE); 
        btnLogout.setFocusPainted(false);
        btnLogout.setFont(new Font("Arial", Font.BOLD, 12));
        pnlHeaderRight.add(btnLogout);

        pnlHeader.add(pnlHeaderRight, BorderLayout.EAST);

        // Theme switching event
        btnTheme.addActionListener(e -> {
            Color newThemeColor = JColorChooser.showDialog(this, "Select Application Theme", pnlHeader.getBackground());
            if (newThemeColor != null) {
                double luminance = (0.299 * newThemeColor.getRed() + 0.587 * newThemeColor.getGreen() + 0.114 * newThemeColor.getBlue()) / 255;
                Color textColor = (luminance < 0.5) ? Color.WHITE : Color.BLACK;
                
                lblWelcome.setForeground(textColor);
                applyGlobalTheme(this.getContentPane(), newThemeColor, textColor); // Recursive coloring
                SwingUtilities.updateComponentTreeUI(this);
            }
        });

        // Logout event
        btnLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                this.dispose(); 
                new LoginFrame().setVisible(true); 
            }
        });

        add(pnlHeader, BorderLayout.NORTH);

        // ==========================================
        // MAIN TABS
        // ==========================================
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.addTab("🎵 Track Management", createTrackTab());
        tabbedPane.addTab("🎤 Artist Management", createArtistTab()); 
        tabbedPane.addTab("📊 Statistics (Charts)", createChartTab());
        
        // Only Admin can see Account Management
        if ("Admin".equalsIgnoreCase(user.role())) {
            tabbedPane.addTab("🔑 Account Management", createUserTab());
        }
        
        add(tabbedPane, BorderLayout.CENTER);
        
        loadTrackData();
        loadArtistData();
    }

    // ==========================================
    // AREA 1: TRACK MANAGEMENT TAB
    // ==========================================
    private void loadTrackData() {
        trackTableModel.setRowCount(0); 
        int offset = currentPage * PAGE_SIZE;
        List<Track> tracks = trackDAO.getTracks(offset, PAGE_SIZE, currentSearchCol, currentKeyword);
        for (Track t : tracks) {
            trackTableModel.addRow(new Object[]{
                t.trackId(), t.trackName(), 
                t.artistName() == null ? "N/A" : t.artistName(),
                t.albumName() == null ? "N/A" : t.albumName(), 
                t.genreName() == null ? "N/A" : t.genreName(), 
                t.durationMs(), t.popularity()
            });
        }
        lblPage.setText("Page: " + (currentPage + 1));
        lblTotalTracks.setText(" | Total results: " + trackDAO.getTotalTracks(currentSearchCol, currentKeyword) + " tracks");
    }

    private JPanel createTrackTab() {
        JPanel pnlTrack = new JPanel(new BorderLayout(10, 10));
        pnlTrack.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTop.add(new JLabel("Search by: "));
        String[] searchOptions = {"All", "Track Name", "Album", "Genre", "ID"};
        JComboBox<String> cbxSearch = new JComboBox<>(searchOptions);
        pnlTop.add(cbxSearch);
        JTextField txtSearch = new JTextField(20);
        pnlTop.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        pnlTop.add(btnSearch);
        JButton btnRefresh = new JButton("Refresh Data");
        pnlTop.add(btnRefresh);
        lblTotalTracks = new JLabel(" | Total results: 0 tracks");
        lblTotalTracks.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotalTracks.setForeground(Color.BLUE);
        pnlTop.add(lblTotalTracks);
        pnlTrack.add(pnlTop, BorderLayout.NORTH);

        String[] columnNames = {"Track ID", "Track Name", "Artist", "Album", "Genre", "Duration (ms)", "Popularity"};
        trackTableModel = new DefaultTableModel(columnNames, 0);
        tblTracks = new JTable(trackTableModel);
        tblTracks.setRowHeight(25);
        rowSorter = new TableRowSorter<>(trackTableModel);
        tblTracks.setRowSorter(rowSorter);
        pnlTrack.add(new JScrollPane(tblTracks), BorderLayout.CENTER);

        JPanel pnlBottomContainer = new JPanel(new BorderLayout());
        
        // CRUD Buttons
        JPanel pnlCRUD = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Add New");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        pnlCRUD.add(btnAdd); pnlCRUD.add(btnEdit); pnlCRUD.add(btnDelete);
        
        // Export Excel Button (Multithreading)
        JButton btnExportExcel = new JButton("Export to Excel (.xlsx)");
        btnExportExcel.setBackground(new Color(40, 167, 69)); 
        btnExportExcel.setForeground(Color.WHITE);
        pnlCRUD.add(btnExportExcel);
        
        // RBAC: Disable buttons for standard User
        if ("User".equalsIgnoreCase(loggedInUser.role())) {
            btnAdd.setVisible(false); 
            btnEdit.setVisible(false); 
            btnDelete.setVisible(false);
        }
        
        JPanel pnlPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrev = new JButton("<< Previous");
        lblPage = new JLabel("Page: 1");
        lblPage.setFont(new Font("Arial", Font.BOLD, 14));
        JButton btnNext = new JButton("Next >>");
        pnlPagination.add(btnPrev); pnlPagination.add(lblPage); pnlPagination.add(btnNext);
        
        pnlBottomContainer.add(pnlCRUD, BorderLayout.WEST);
        pnlBottomContainer.add(pnlPagination, BorderLayout.EAST);
        pnlTrack.add(pnlBottomContainer, BorderLayout.SOUTH);

        // --- TRACK TAB EVENTS ---
        btnSearch.addActionListener(e -> {
            currentSearchCol = cbxSearch.getSelectedItem().toString();
            currentKeyword = txtSearch.getText();
            currentPage = 0; loadTrackData();
        });
        
        btnRefresh.addActionListener(e -> {
            txtSearch.setText(""); cbxSearch.setSelectedIndex(0);
            currentSearchCol = "All"; currentKeyword = ""; currentPage = 0;
            loadTrackData(); JOptionPane.showMessageDialog(this, "Data synchronized successfully!");
        });

        btnPrev.addActionListener(e -> { if (currentPage > 0) { currentPage--; loadTrackData(); } });
        btnNext.addActionListener(e -> {
            currentPage++;
            if (trackDAO.getTracks(currentPage * PAGE_SIZE, PAGE_SIZE, currentSearchCol, currentKeyword).isEmpty()) {
                currentPage--; JOptionPane.showMessageDialog(this, "This is the last page!");
            } else { loadTrackData(); }
        });

        btnAdd.addActionListener(e -> {
            TrackDialog dialog = new TrackDialog(this, "Add New Track", null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                Object[] data = dialog.getTrackData();
                try {
                    Track newTrack = new Track(data[0].toString(), data[1].toString(), data[2].toString(), null, null, Integer.parseInt(data[5].toString()), Integer.parseInt(data[6].toString()));
                    if (trackDAO.insert(newTrack)) {
                        JOptionPane.showMessageDialog(this, "Added successfully!"); loadTrackData(); 
                    } else JOptionPane.showMessageDialog(this, "Error: Track ID already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid number format!"); }
            }
        });
        
        btnEdit.addActionListener(e -> {
            int row = tblTracks.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a track to edit!"); return; }
            Object[] currentData = new Object[7]; 
            for (int i = 0; i < 7; i++) currentData[i] = trackTableModel.getValueAt(tblTracks.convertRowIndexToModel(row), i);
            
            TrackDialog dialog = new TrackDialog(this, "Edit Track", currentData);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                Object[] data = dialog.getTrackData();
                try {
                    Track updatedTrack = new Track(data[0].toString(), data[1].toString(), data[2].toString(), null, null, Integer.parseInt(data[5].toString()), Integer.parseInt(data[6].toString()));
                    if (trackDAO.update(updatedTrack)) { JOptionPane.showMessageDialog(this, "Updated successfully!"); loadTrackData(); }
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid number format!"); }
            }
        });
        
        btnDelete.addActionListener(e -> {
            int row = tblTracks.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a track to delete!"); return; }
            if (JOptionPane.showConfirmDialog(this, "Delete this track permanently?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String trackId = trackTableModel.getValueAt(tblTracks.convertRowIndexToModel(row), 0).toString();
                if (trackDAO.delete(trackId)) { JOptionPane.showMessageDialog(this, "Deleted successfully!"); loadTrackData(); }
            }
        });

        // Excel Export Event (SwingWorker implementation for background processing)
        btnExportExcel.addActionListener(e -> {
            if (tblTracks.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No data available to export!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select location to save Excel file");
            fileChooser.setSelectedFile(new File("Spotify_Tracks_Data.xlsx"));
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".xlsx")) filePath += ".xlsx";
                final String finalPath = filePath;
                btnExportExcel.setEnabled(false); 
                
                SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        try (Workbook workbook = new XSSFWorkbook()) {
                            Sheet sheet = workbook.createSheet("Spotify Tracks");

                            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                            headerFont.setBold(true);
                            CellStyle headerCellStyle = workbook.createCellStyle();
                            headerCellStyle.setFont(headerFont);
                            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                            Row headerRow = sheet.createRow(0);
                            for (int i = 0; i < tblTracks.getColumnCount(); i++) {
                                Cell cell = headerRow.createCell(i);
                                cell.setCellValue(tblTracks.getColumnName(i));
                                cell.setCellStyle(headerCellStyle);
                            }

                            int rowCount = tblTracks.getRowCount();
                            int totalPopularity = 0;

                            for (int r = 0; r < rowCount; r++) {
                                Row row = sheet.createRow(r + 1);
                                for (int c = 0; c < tblTracks.getColumnCount(); c++) {
                                    Cell cell = row.createCell(c);
                                    Object val = tblTracks.getValueAt(r, c);
                                    
                                    if (val instanceof Number) {
                                        cell.setCellValue(((Number) val).doubleValue());
                                        if (c == 6) totalPopularity += ((Number) val).intValue();
                                    } else {
                                        cell.setCellValue(val == null ? "" : val.toString());
                                    }
                                }
                            }

                            int summaryRowIndex = rowCount + 2; 
                            Row summaryRow = sheet.createRow(summaryRowIndex);
                            
                            org.apache.poi.ss.usermodel.Font summaryFont = workbook.createFont();
                            summaryFont.setBold(true);
                            CellStyle summaryStyle = workbook.createCellStyle();
                            summaryStyle.setFont(summaryFont);

                            Cell labelCell = summaryRow.createCell(1);
                            labelCell.setCellValue("DATA SUMMARY:");
                            labelCell.setCellStyle(summaryStyle);

                            Cell countCell = summaryRow.createCell(2);
                            countCell.setCellValue("Total Tracks: " + rowCount);
                            countCell.setCellStyle(summaryStyle);

                            Cell avgPopCell = summaryRow.createCell(6);
                            double avgPop = rowCount > 0 ? (double) totalPopularity / rowCount : 0;
                            avgPopCell.setCellValue("Avg Popularity: " + Math.round(avgPop * 10.0) / 10.0);
                            avgPopCell.setCellStyle(summaryStyle);

                            for (int i = 0; i < tblTracks.getColumnCount(); i++) {
                                sheet.autoSizeColumn(i);
                            }

                            try (FileOutputStream fileOut = new FileOutputStream(finalPath)) {
                                workbook.write(fileOut);
                            }
                            return true;
                        }
                    }

                    @Override
                    protected void done() {
                        btnExportExcel.setEnabled(true);
                        try {
                            if (get()) {
                                JOptionPane.showMessageDialog(DashboardFrame.this, 
                                    "Export completed successfully!\nPath: " + finalPath, "Success", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(DashboardFrame.this, "Error: " + ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        });

        return pnlTrack;
    }

    // ==========================================
    // AREA 2: ARTIST MANAGEMENT TAB
    // ==========================================
    private void loadArtistData() {
        artistTableModel.setRowCount(0); 
        int offset = currentArtistPage * PAGE_SIZE;
        List<Artist> artists = artistDAO.getArtists(offset, PAGE_SIZE, currentArtistKeyword);
        for (Artist a : artists) { artistTableModel.addRow(new Object[]{ a.artistId(), a.artistName() }); }
        lblArtistPage.setText("Page: " + (currentArtistPage + 1));
        lblTotalArtists.setText(" | Total artists: " + artistDAO.getTotalArtists(currentArtistKeyword));
    }

    private JPanel createArtistTab() {
        JPanel pnlArtist = new JPanel(new BorderLayout(10, 10));
        pnlArtist.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTop.add(new JLabel("Search Artist Name: "));
        JTextField txtSearch = new JTextField(25);
        pnlTop.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        JButton btnRefresh = new JButton("Refresh");
        pnlTop.add(btnSearch); pnlTop.add(btnRefresh);
        lblTotalArtists = new JLabel(" | Total artists: 0");
        lblTotalArtists.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotalArtists.setForeground(Color.BLUE);
        pnlTop.add(lblTotalArtists);
        pnlArtist.add(pnlTop, BorderLayout.NORTH);

        String[] columnNames = {"Artist ID", "Artist Name"};
        artistTableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblArtists = new JTable(artistTableModel);
        tblArtists.setRowHeight(25);
        pnlArtist.add(new JScrollPane(tblArtists), BorderLayout.CENTER);

        JPanel pnlBottomContainer = new JPanel(new BorderLayout());
        JPanel pnlCRUD = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Add Artist");
        JButton btnEdit = new JButton("Edit Name");
        JButton btnDelete = new JButton("Delete Artist");
        pnlCRUD.add(btnAdd); pnlCRUD.add(btnEdit); pnlCRUD.add(btnDelete);
        
        if ("User".equalsIgnoreCase(loggedInUser.role())) {
            btnAdd.setVisible(false); btnEdit.setVisible(false); btnDelete.setVisible(false);
        }

        JPanel pnlPagination = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrev = new JButton("<< Previous");
        lblArtistPage = new JLabel("Page: 1");
        lblArtistPage.setFont(new Font("Arial", Font.BOLD, 14));
        JButton btnNext = new JButton("Next >>");
        pnlPagination.add(btnPrev); pnlPagination.add(lblArtistPage); pnlPagination.add(btnNext);
        
        pnlBottomContainer.add(pnlCRUD, BorderLayout.WEST);
        pnlBottomContainer.add(pnlPagination, BorderLayout.EAST);
        pnlArtist.add(pnlBottomContainer, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> { currentArtistKeyword = txtSearch.getText(); currentArtistPage = 0; loadArtistData(); });
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); currentArtistKeyword = ""; currentArtistPage = 0; loadArtistData(); });
        btnPrev.addActionListener(e -> { if (currentArtistPage > 0) { currentArtistPage--; loadArtistData(); } });
        btnNext.addActionListener(e -> {
            currentArtistPage++;
            if (artistDAO.getArtists(currentArtistPage * PAGE_SIZE, PAGE_SIZE, currentArtistKeyword).isEmpty()) {
                currentArtistPage--; JOptionPane.showMessageDialog(this, "End of list!");
            } else loadArtistData();
        });
        
        btnAdd.addActionListener(e -> {
            ArtistDialog dialog = new ArtistDialog(this, "Add New Artist", -1, ""); dialog.setVisible(true);
            if (dialog.isSaved()) {
                Artist newArtist = new Artist(0, dialog.getArtistName());
                if (artistDAO.insert(newArtist)) { JOptionPane.showMessageDialog(this, "Added successfully!"); loadArtistData(); }
            }
        });
        
        btnEdit.addActionListener(e -> {
            int row = tblArtists.getSelectedRow(); if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an artist!"); return; }
            int id = (int) artistTableModel.getValueAt(row, 0); String name = (String) artistTableModel.getValueAt(row, 1);
            ArtistDialog dialog = new ArtistDialog(this, "Edit Artist", id, name); dialog.setVisible(true);
            if (dialog.isSaved()) {
                Artist updatedArtist = new Artist(id, dialog.getArtistName());
                if (artistDAO.update(updatedArtist)) { JOptionPane.showMessageDialog(this, "Updated successfully!"); loadArtistData(); }
            }
        });
        
        btnDelete.addActionListener(e -> {
            int row = tblArtists.getSelectedRow(); if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an artist!"); return; }
            int id = (int) artistTableModel.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(this, "Delete this artist?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (artistDAO.delete(id)) { JOptionPane.showMessageDialog(this, "Deleted successfully!"); loadArtistData(); }
            }
        });
        return pnlArtist;
    }

    // ==========================================
    // AREA 3: DATA VISUALIZATION TAB (CHARTS)
    // ==========================================
    private JPanel createChartTab() {
        JPanel pnlChartMain = new JPanel(new BorderLayout());
        
        JPanel pnlChartsContainer = new JPanel(new GridLayout(1, 2, 10, 10));
        pnlChartsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Bar Chart (Top Tracks)
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        
        // --- MODERN JAVA: var, Lambda and Stream API ---
        var topTracks = trackDAO.getTop5PopularTracks();
        topTracks.stream()
                 .filter(t -> t.popularity() > 0) 
                 .forEach(t -> barDataset.addValue(t.popularity(), "Popularity Score", t.trackName()));
        
        JFreeChart barChart = ChartFactory.createBarChart("Top 5 Most Popular Tracks", "Track Name", "Popularity Score", barDataset);
        pnlChartsContainer.add(new ChartPanel(barChart));

        // 2. Pie Chart (Top Artists)
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        Map<String, Integer> topArtists = artistDAO.getTop5PopularArtists();
        for (Map.Entry<String, Integer> entry : topArtists.entrySet()) {
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
        JFreeChart pieChart = ChartFactory.createPieChart("Top 5 Artists (Total Popularity)", pieDataset, true, true, false);
        pnlChartsContainer.add(new ChartPanel(pieChart));

        pnlChartMain.add(pnlChartsContainer, BorderLayout.CENTER);
        
        JPanel pnlBottom = new JPanel();
        JButton btnRefresh = new JButton("Refresh Chart Data");
        pnlBottom.add(btnRefresh);
        
        btnRefresh.addActionListener(e -> {
            barDataset.clear(); 
            trackDAO.getTop5PopularTracks().stream()
                 .filter(t -> t.popularity() > 0) 
                 .forEach(t -> barDataset.addValue(t.popularity(), "Popularity Score", t.trackName()));
            
            pieDataset.clear();
            Map<String, Integer> newTopArtists = artistDAO.getTop5PopularArtists();
            for (Map.Entry<String, Integer> entry : newTopArtists.entrySet()) {
                pieDataset.setValue(entry.getKey(), entry.getValue());
            }
            JOptionPane.showMessageDialog(this, "Charts successfully updated from the database!");
        });
        
        pnlChartMain.add(pnlBottom, BorderLayout.SOUTH);
        return pnlChartMain;
    }

    // ==========================================
    // AREA 4: ACCOUNT MANAGEMENT TAB (ADMIN ONLY)
    // ==========================================
    private JPanel createUserTab() {
        JPanel pnlUser = new JPanel(new BorderLayout(10, 10));
        pnlUser.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        UserDAO userDAO = new UserDAO();

        String[] columnNames = {"User ID", "Username", "Role"};
        DefaultTableModel userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tblUsers = new JTable(userTableModel);
        tblUsers.setRowHeight(25);
        pnlUser.add(new JScrollPane(tblUsers), BorderLayout.CENTER);

        Runnable loadUsers = () -> {
            userTableModel.setRowCount(0);
            for (User u : userDAO.getAllUsers()) {
                userTableModel.addRow(new Object[]{u.userId(), u.username(), u.role()});
            }
        };
        loadUsers.run(); 

        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnEditRole = new JButton("Edit Role / Reset Password");
        JButton btnDeleteUser = new JButton("Delete Account");
        pnlBottom.add(btnEditRole); pnlBottom.add(btnDeleteUser);
        pnlUser.add(pnlBottom, BorderLayout.SOUTH);

        btnEditRole.addActionListener(e -> {
            int row = tblUsers.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an account to edit!"); return; }
            int id = (int) userTableModel.getValueAt(row, 0);
            String currentRole = (String) userTableModel.getValueAt(row, 2);

            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("New Role:"));
            JComboBox<String> cbxRole = new JComboBox<>(new String[]{"Admin", "Staff", "User"});
            cbxRole.setSelectedItem(currentRole);
            panel.add(cbxRole);
            panel.add(new JLabel("New Password (Leave blank to keep current):"));
            JPasswordField txtPass = new JPasswordField();
            panel.add(txtPass);

            if (JOptionPane.showConfirmDialog(this, panel, "Update Account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                String newPass = new String(txtPass.getPassword());
                if (userDAO.updateUser(id, cbxRole.getSelectedItem().toString(), newPass)) {
                    JOptionPane.showMessageDialog(this, "Account updated successfully!");
                    loadUsers.run();
                }
            }
        });

        btnDeleteUser.addActionListener(e -> {
            int row = tblUsers.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an account to delete!"); return; }
            int id = (int) userTableModel.getValueAt(row, 0);
            
            if (id == loggedInUser.userId()) {
                JOptionPane.showMessageDialog(this, "You cannot delete your currently active account!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (JOptionPane.showConfirmDialog(this, "Permanently delete this account?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (userDAO.deleteUser(id)) { JOptionPane.showMessageDialog(this, "Account deleted!"); loadUsers.run(); }
            }
        });

        return pnlUser;
    }

    // ==========================================
    // RECURSIVE ALGORITHM: GLOBAL THEME APPLICATION
    // ==========================================
    private void applyGlobalTheme(Container container, Color bg, Color textColor) {
        if (container instanceof JPanel || container instanceof JScrollPane || 
            container instanceof JTabbedPane || container instanceof JViewport) {
            container.setBackground(bg);
        }
        
        if (container instanceof JLabel) {
            container.setForeground(textColor);
        }

        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                if (!(c instanceof JTable) && !(c instanceof JTextField)) {
                    applyGlobalTheme((Container) c, bg, textColor);
                }
            }
        }
    }
} 

// =======================================================
// SECONDARY WINDOW CLASSES (JDIALOGS) FOR DATA ENTRY
// =======================================================

class TrackDialog extends JDialog {
    private JTextField txtId, txtName, txtArtist, txtAlbum, txtGenre, txtDuration, txtPopularity;
    private JButton btnSave, btnCancel;
    private boolean saved = false;
    private Object[] trackData = new Object[7]; 

    public TrackDialog(Frame parent, String title, Object[] currentData) {
        super(parent, title, true);
        setSize(400, 350); setLocationRelativeTo(parent); setLayout(new GridLayout(8, 2, 10, 10));

        txtId = new JTextField(); txtName = new JTextField(); txtArtist = new JTextField(); 
        txtAlbum = new JTextField(); txtGenre = new JTextField(); 
        txtDuration = new JTextField(); txtPopularity = new JTextField();

        if (currentData != null) {
            txtId.setText(currentData[0].toString()); txtId.setEditable(false);
            txtName.setText(currentData[1].toString());
            txtArtist.setText(currentData[2] == null ? "" : currentData[2].toString()); 
            txtAlbum.setText(currentData[3] == null ? "" : currentData[3].toString());
            txtGenre.setText(currentData[4] == null ? "" : currentData[4].toString());
            txtDuration.setText(currentData[5].toString());
            txtPopularity.setText(currentData[6].toString());
        }

        add(new JLabel("  Track ID:")); add(txtId);
        add(new JLabel("  Track Name:")); add(txtName);
        add(new JLabel("  Artist:")); add(txtArtist); 
        add(new JLabel("  Album:")); add(txtAlbum);
        add(new JLabel("  Genre:")); add(txtGenre);
        add(new JLabel("  Duration (ms):")); add(txtDuration);
        add(new JLabel("  Popularity:")); add(txtPopularity);

        btnSave = new JButton("Save"); btnCancel = new JButton("Cancel");
        add(btnSave); add(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        btnSave.addActionListener(e -> {
            if (txtId.getText().isEmpty() || txtName.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Track ID and Name are required!"); return;
            }
            trackData[0] = txtId.getText(); trackData[1] = txtName.getText(); trackData[2] = txtArtist.getText(); 
            trackData[3] = txtAlbum.getText();  trackData[4] = txtGenre.getText();
            trackData[5] = txtDuration.getText(); trackData[6] = txtPopularity.getText();
            saved = true; dispose();
        });
    }
    public boolean isSaved() { return saved; }
    public Object[] getTrackData() { return trackData; }
}

class ArtistDialog extends JDialog {
    private JTextField txtName; private JButton btnSave, btnCancel; private boolean saved = false;
    public ArtistDialog(Frame parent, String title, int id, String name) {
        super(parent, title, true); setSize(350, 150); setLocationRelativeTo(parent); setLayout(new GridLayout(3, 2, 10, 10));
        add(new JLabel("  Artist ID:"));
        JTextField txtId = new JTextField(id == -1 ? "Auto-increment" : String.valueOf(id)); txtId.setEditable(false); add(txtId);
        add(new JLabel("  Artist Name:")); txtName = new JTextField(name); add(txtName);
        btnSave = new JButton("Save"); btnCancel = new JButton("Cancel"); add(btnSave); add(btnCancel);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Artist name cannot be empty!"); return; }
            saved = true; dispose();
        });
    }
    public boolean isSaved() { return saved; }
    public String getArtistName() { return txtName.getText().trim(); }
}