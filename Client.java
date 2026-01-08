package Sad;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Client {

   
    private static final int SERVER_PORT = 3000;
    private static final String SERVER_HOST = "localhost";
    private static final String POIDS_ROW_NAME = "Poids";

    
    private static final class Style {
        
        static final Color PRIMARY_COLOR = new Color(0, 150, 136); 
        static final Color DANGER_COLOR = new Color(244, 67, 54);   
        static final Color SUCCESS_COLOR = new Color(76, 175, 80);  

        
        static final Color GRADIENT_START = new Color(238, 242, 243);
        static final Color GRADIENT_END = new Color(214, 228, 235);
        static final Color GLASS_PANE_COLOR = new Color(255, 255, 255, 70);
        static final Color PANEL_BACKGROUND = Color.WHITE;

        static final Color TEXT_COLOR = new Color(38, 50, 56);
        static final Color BORDER_COLOR = new Color(207, 216, 220, 150);
        static final Color BORDER_FOCUS_COLOR = PRIMARY_COLOR;

        
        static final Color SELECTION_COLOR = new Color(224, 242, 241);
        static final Color WEIGHT_ROW_BACKGROUND = new Color(255, 248, 225);
        static final Color WEIGHT_ROW_FOREGROUND = new Color(191, 123, 0);

        
        static final Font MAIN_FONT = new Font("Inter", Font.PLAIN, 15);
        static final Font BOLD_FONT = new Font("Inter", Font.BOLD, 15);
        static final Font TITLE_FONT = new Font("Inter", Font.BOLD, 26);
        static final Font HEADER_FONT = new Font("Inter", Font.BOLD, 16);
        static final Font BUTTON_FONT = new Font("Inter", Font.BOLD, 14);

        
        static final int PADDING = 25;
    }

    
    private static final Map<String, String> STRINGS = new HashMap<>();
    static {
        STRINGS.put("title", "Projet SAD - Méthode ÉLECTRE I");
        STRINGS.put("tableTitle", "Matrice de Décision");
        STRINGS.put("concordanceSeuil", "Seuil Concordance (C) :");
        STRINGS.put("discordanceSeuil", "Seuil Discordance (D) :");
        STRINGS.put("addButton", "Ajouter");
        STRINGS.put("removeButton", "Supprimer");
        STRINGS.put("calcButton", "Lancer le Calcul");
        STRINGS.put("errorTitle", "Erreur");
        STRINGS.put("warningTitle", "Avertissement");
    }

    
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField seuilCField;
    private JTextField seuilDField;

    public Client() {

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        initializeLookAndFeel();
        initializeFrame();
        buildUI();
        frame.setVisible(true);
    }

    private void initializeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set LookAndFeel.");
        }
    }

    private void initializeFrame() {
        frame = new JFrame(STRINGS.get("title"));
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setSize(1300, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

    private void buildUI() {
        initializeTable();

        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(Style.PADDING, Style.PADDING));
        mainPanel.setBorder(new EmptyBorder(Style.PADDING, Style.PADDING, Style.PADDING, Style.PADDING));

        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createMainContentPanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
    }

    private void initializeTable() {
        String[] criteres = {"Alternative", "Sites à visiter", "Restauration", "Hébergement", "Shopping"};
        Object[] poidsRow = {"Poids", "1", "1", "1", "1"};

        model = new DefaultTableModel(new Object[][]{poidsRow}, criteres);
        table = new JTable(model);
    }

    private void styleTable() {
        table.setRowHeight(48);
        table.setFont(Style.MAIN_FONT);
        table.setGridColor(Style.BORDER_COLOR);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Style.PANEL_BACKGROUND);
        table.setSelectionBackground(Style.SELECTION_COLOR);
        table.setSelectionForeground(Style.TEXT_COLOR);

        JTableHeader header = table.getTableHeader();
        header.setFont(Style.HEADER_FONT);
        header.setBackground(Style.PANEL_BACKGROUND);
        header.setForeground(Style.TEXT_COLOR);
        header.setPreferredSize(new Dimension(0, 55));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Style.BORDER_COLOR));

      
        header.addMouseListener(new MouseAdapter() {
            
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int columnIndex = header.columnAtPoint(e.getPoint());
                    if (columnIndex == 0) return; 

                    TableColumn tableColumn = header.getColumnModel().getColumn(columnIndex);
                    String currentName = tableColumn.getHeaderValue().toString();

                    String newName = (String) JOptionPane.showInputDialog(
                            frame,
                            "Entrez le nouveau nom pour le critère :",
                            "Modifier le Critère",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            currentName
                    );

                    if (newName != null && !newName.trim().isEmpty()) {
                        renameColumn(columnIndex, newName);
                    }
                }
            }
        });

        applyRenderers();
    }

    private void applyRenderers() {
        AlternatingRowRendererContrast renderer = new AlternatingRowRendererContrast();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
    }

    private void renameColumn(int index, String newName) {
        String[] identifiers = new String[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            identifiers[i] = table.getColumnModel().getColumn(i).getHeaderValue().toString();
        }
        identifiers[index] = newName;
        model.setColumnIdentifiers(identifiers);
        applyRenderers();
    }

    private JPanel createMainContentPanel() {
        JPanel mainPanel = new RoundedPanel(20);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Style.PANEL_BACKGROUND);
        mainPanel.setOpaque(false);

        JLabel tableTitle = new JLabel(STRINGS.get("tableTitle"), SwingConstants.CENTER);
        tableTitle.setFont(Style.TITLE_FONT);
        tableTitle.setForeground(Style.TEXT_COLOR);
        tableTitle.setBorder(new EmptyBorder(Style.PADDING, 0, Style.PADDING, 0));

        styleTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Style.PANEL_BACKGROUND);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new JPanel());

        mainPanel.add(tableTitle, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new GlassPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 10));

        controlPanel.add(createStyledLabel(STRINGS.get("concordanceSeuil")));
        seuilCField = createStyledTextField("0.6", 5);
        controlPanel.add(seuilCField);

        controlPanel.add(createStyledLabel(STRINGS.get("discordanceSeuil")));
        seuilDField = createStyledTextField("0.4", 5);
        controlPanel.add(seuilDField);

        return controlPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout(20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(15, 0, 10, 0));

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        actionButtons.setOpaque(false);
        JButton addButton = createStyledButton(STRINGS.get("addButton"), Style.PRIMARY_COLOR, SvgIcon.ADD);
        addButton.addActionListener(e -> ajouterAlternative());
        actionButtons.add(addButton);

        JButton removeButton = createStyledButton(STRINGS.get("removeButton"), Style.DANGER_COLOR, SvgIcon.DELETE);
        removeButton.addActionListener(e -> supprimerAlternative());
        actionButtons.add(removeButton);

        JPanel calcButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        calcButtonPanel.setOpaque(false);
        JButton validerButton = createStyledButton(STRINGS.get("calcButton"), Style.SUCCESS_COLOR, SvgIcon.CALCULATE);
        validerButton.setFont(Style.BOLD_FONT.deriveFont(16f));
        validerButton.addActionListener(e -> calculer());
        calcButtonPanel.add(validerButton);

        buttonPanel.add(actionButtons, BorderLayout.WEST);
        buttonPanel.add(calcButtonPanel, BorderLayout.EAST);
        return buttonPanel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Style.BOLD_FONT);
        label.setForeground(Style.TEXT_COLOR);
        return label;
    }

    private JTextField createStyledTextField(String text, int cols) {
        return new CustomTextField(text, cols);
    }

    private JButton createStyledButton(String text, Color background, SvgIcon icon) {
        return new CustomButton(text, background, icon);
    }

    

    static class GradientPanel extends JPanel {
        
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, Style.GRADIENT_START, 0, getHeight(), Style.GRADIENT_END);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        public RoundedPanel(int radius) {
            super();
            this.cornerRadius = radius;
            setOpaque(false);
        }
        
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class GlassPanel extends JPanel {
        public GlassPanel() {
            setOpaque(false);
        }
        
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Style.GLASS_PANE_COLOR);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
            g2.setColor(Style.BORDER_COLOR);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
            g2.dispose();
        }
    }

    static class CustomTextField extends JTextField {
        private boolean hasFocus;

        public CustomTextField(String text, int cols) {
            super(text, cols);
            this.hasFocus = false;
            setFont(Style.MAIN_FONT.deriveFont(16f));
            setHorizontalAlignment(JTextField.CENTER);
            setForeground(Style.TEXT_COLOR);
            setBackground(new Color(0,0,0,0));
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setOpaque(false);

            addFocusListener(new FocusAdapter() {
                
                public void focusGained(FocusEvent e) {
                    hasFocus = true;
                    repaint();
                }
                
                public void focusLost(FocusEvent e) {
                    hasFocus = false;
                    repaint();
                }
            });
        }

        
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Style.PANEL_BACKGROUND);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

            g2.setColor(hasFocus ? Style.BORDER_FOCUS_COLOR : Style.BORDER_COLOR);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class CustomButton extends JButton {
        private final Color backgroundColor;
        private float animation = 0f;

        public CustomButton(String text, Color background, SvgIcon icon) {
            super(text);
            this.backgroundColor = background;
            if (icon != null) {
                setIcon(icon.getIcon(16, Color.WHITE));
                setIconTextGap(10);
            }
            setFont(Style.BUTTON_FONT);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorder(new EmptyBorder(12, 25, 12, 25));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                
                public void mousePressed(MouseEvent e) {
                    animation = 1f;
                    repaint();
                }
                
                public void mouseReleased(MouseEvent e) {
                    animation = 0f;
                    repaint();
                }
            });
        }

        
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg = new Color(
                (int) (backgroundColor.getRed() * (1 - animation * 0.1)),
                (int) (backgroundColor.getGreen() * (1 - animation * 0.1)),
                (int) (backgroundColor.getBlue() * (1 - animation * 0.1))
            );
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    enum SvgIcon {
        ADD, DELETE, CALCULATE;
        private String svgContent;
        static {
            ADD.svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"12\" y1=\"5\" x2=\"12\" y2=\"19\"></line><line x1=\"5\" y1=\"12\" x2=\"19\" y2=\"12\"></line></svg>";
            DELETE.svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polyline points=\"3 6 5 6 21 6\"></polyline><path d=\"M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2\"></path><line x1=\"10\" y1=\"11\" x2=\"10\" y2=\"17\"></line><line x1=\"14\" y1=\"11\" x2=\"14\" y2=\"17\"></line></svg>";
            CALCULATE.svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2\"></path><circle cx=\"8.5\" cy=\"7\" r=\"4\"></circle><polyline points=\"17 11 19 13 23 9\"></polyline></svg>";
        }

        public ImageIcon getIcon(int size, Color color) {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2f));
            try {
                if (this == ADD) {
                    g2d.drawLine(size / 2, 4, size / 2, size - 4);
                    g2d.drawLine(4, size / 2, size - 4, size / 2);
                } else if (this == DELETE) {
                    g2d.drawRect(5, 7, size - 10, size - 9);
                    g2d.drawLine(4, 5, size - 4, 5);
                    g2d.drawLine(size/2 - 3, 3, size/2 + 3, 3);
                    g2d.drawLine(size/2 - 2, 10, size/2 - 2, size - 6);
                    g2d.drawLine(size/2 + 2, 10, size/2 + 2, size - 6);
                } else { // CALCULATE
                    g2d.drawOval(size/2 - 6, 4, 10, 10);
                    g2d.drawArc(2, 10, size - 4, 12, 180, 180);
                }
            } finally {
                g2d.dispose();
            }
            return new ImageIcon(image);
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    

    private void ajouterAlternative() {
        int n = model.getRowCount();
        int insertIndex = (n > 0 && Objects.equals(model.getValueAt(n - 1, 0), POIDS_ROW_NAME)) ? n - 1 : n;

        String newName = "Nouvelle Destination " + (insertIndex);
        Object[] newRow = new Object[model.getColumnCount()];
        newRow[0] = newName;
        Arrays.fill(newRow, 1, model.getColumnCount(), "0");

        model.insertRow(insertIndex, newRow);
    }

    private void supprimerAlternative() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            if (Objects.equals(model.getValueAt(selectedRow, 0), POIDS_ROW_NAME)) {
                JOptionPane.showMessageDialog(frame, "Vous ne pouvez pas supprimer la ligne des poids.", STRINGS.get("warningTitle"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            model.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(frame, "Veuillez sélectionner une ligne à supprimer.", STRINGS.get("warningTitle"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private static double parseDoubleSafe(Object val) {
        if (val == null) return 0.0;
        try {
            String s = val.toString().trim();
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "La valeur '" + val + "' n'est pas un nombre valide et sera traitée comme 0.0.", "Correction de Donnée", JOptionPane.WARNING_MESSAGE);
            return 0.0;
        }
    }

    private double[][] extraireDonneesTableau() {
        int totalRows = model.getRowCount();
        int dataRows = 0;
        for (int i = 0; i < totalRows; i++) {
            if (!Objects.equals(model.getValueAt(i, 0), POIDS_ROW_NAME)) {
                dataRows++;
            }
        }

        if (dataRows == 0) return new double[0][0];

        int totalCols = model.getColumnCount() - 1;
        double[][] data = new double[dataRows][totalCols];
        int currentRow = 0;
        for (int i = 0; i < totalRows; i++) {
            if (!Objects.equals(model.getValueAt(i, 0), POIDS_ROW_NAME)) {
                for (int j = 1; j <= totalCols; j++) {
                    data[currentRow][j - 1] = parseDoubleSafe(model.getValueAt(i, j));
                }
                currentRow++;
            }
        }
        return data;
    }

    private double[] extrairePoids() {
        int totalCols = model.getColumnCount() - 1;
        double[] poids = new double[totalCols];
        int rowPoids = -1;

        for (int i = 0; i < model.getRowCount(); i++) {
            if (Objects.equals(model.getValueAt(i, 0), POIDS_ROW_NAME)) {
                rowPoids = i;
                break;
            }
        }

        if (rowPoids == -1) {
            Arrays.fill(poids, 1.0 / totalCols);
            return poids;
        }

        double sum = 0.0;
        for (int j = 1; j <= totalCols; j++) {
            poids[j - 1] = parseDoubleSafe(model.getValueAt(rowPoids, j));
            sum += poids[j - 1];
        }

        if (sum == 0.0) {
            Arrays.fill(poids, 1.0 / totalCols);
        } else {
            for (int i = 0; i < poids.length; i++) poids[i] /= sum;
        }
        return poids;
    }

    private double parseDoubleSeuil(String text, String seuilName) throws IllegalArgumentException {
        try {
            double value = Double.parseDouble(text.replace(',', '.'));
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Erreur : Le " + seuilName + " doit être un nombre entre 0 et 1.");
            }
            return value;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Erreur : Le " + seuilName + " doit être un nombre valide.");
        }
    }

    

    private void calculer() {
        double[][] data = extraireDonneesTableau();
        if (data.length == 0) {
            JOptionPane.showMessageDialog(frame, "Veuillez ajouter au moins une alternative (ligne).", STRINGS.get("warningTitle"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double seuilC = parseDoubleSeuil(seuilCField.getText(), "Seuil C");
            double seuilD = parseDoubleSeuil(seuilDField.getText(), "Seuil D");

            new CalculationWorker(data, extrairePoids(), seuilC, seuilD).execute();

        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(frame, iae.getMessage(), STRINGS.get("errorTitle"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private class CalculationWorker extends SwingWorker<Void, Void> {
        private final double[][] data;
        private final double[] poids;
        private final double seuilC;
        private final double seuilD;

        public CalculationWorker(double[][] data, double[] poids, double seuilC, double seuilD) {
            this.data = data;
            this.poids = poids;
            this.seuilC = seuilC;
            this.seuilD = seuilD;
        }

        
        protected Void doInBackground() {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                oos.writeObject(data);
                oos.writeObject(poids);
                oos.flush();

                double[][] concordance = (double[][]) ois.readObject();
                double[][] discordance = (double[][]) ois.readObject();

                String surclassement = calculElectre(concordance, discordance, seuilC, seuilD);
                afficherMatrices(concordance, discordance, surclassement, seuilC, seuilD);

            } catch (ConnectException ce) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Erreur : Le serveur ÉLECTRE n'est pas démarré sur le port " + SERVER_PORT + ".",
                        "Erreur de Connexion", JOptionPane.ERROR_MESSAGE));
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Une erreur inattendue est survenue lors du calcul: " + ex.getMessage(),
                        STRINGS.get("errorTitle"), JOptionPane.ERROR_MESSAGE));
            }
            return null;
        }
    }

    private String getAlternativeName(int index) {
        int dataIndex = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (!Objects.equals(model.getValueAt(i, 0), POIDS_ROW_NAME)) {
                if (dataIndex == index) {
                    return model.getValueAt(i, 0).toString();
                }
                dataIndex++;
            }
        }
        return "A" + (index + 1);
    }

    private String calculElectre(double[][] concordance, double[][] discordance, double seuilC, double seuilD) {
        int dataRows = extraireDonneesTableau().length;
        StringBuilder resultat = new StringBuilder();

        for (int i = 0; i < dataRows; i++) {
            for (int j = 0; j < dataRows; j++) {
                if (i == j) continue;

                if (concordance[i][j] >= seuilC && discordance[i][j] <= seuilD) {
                    resultat.append(getAlternativeName(i)).append(" surclasse ").append(getAlternativeName(j)).append("\n");
                }
            }
        }
        if (resultat.length() == 0) return "Aucun surclassement détecté avec les seuils actuels.";
        return resultat.toString();
    }

    private String trouverMeilleuresAlternatives(double[][] conc, double[][] disc, double seuilC, double seuilD) {
        int n = conc.length;
        if (n == 0) return "Aucune alternative n'a été trouvée.";

        boolean[][] surclassementBinaire = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    surclassementBinaire[i][j] = (conc[i][j] >= seuilC && disc[i][j] <= seuilD);
                }
            }
        }

        boolean[] estDansLeNoyau = new boolean[n];
        Arrays.fill(estDansLeNoyau, true);

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (i != j && surclassementBinaire[i][j]) {
                    estDansLeNoyau[j] = false;
                    break;
                }
            }
        }

        String meilleurNom = "Non défini";
        int maxSurclassement = -1;

        StringBuilder candidatsNoyau = new StringBuilder();
        int nombreCandidats = 0;

        for (int i = 0; i < n; i++) {
            if (estDansLeNoyau[i]) {
                nombreCandidats++;
                String nom = getAlternativeName(i);

                if (candidatsNoyau.length() > 0) candidatsNoyau.append(", ");
                candidatsNoyau.append("<b>").append(nom).append("</b>");

                int surclassements = 0;
                for (int j = 0; j < n; j++) {
                    if (surclassementBinaire[i][j]) {
                        surclassements++;
                    }
                }

                if (surclassements > maxSurclassement) {
                    maxSurclassement = surclassements;
                    meilleurNom = nom;
                }
            }
        }

        if (nombreCandidats == 0) {
            return "Aucune destination n'est dans le Noyau (toutes sont surclassées).";
        } else if (nombreCandidats == 1) {
            return "La meilleure destination est <b>" + meilleurNom + "</b> (unique dans le Noyau).";
        } else {
            return "Le Noyau contient : " + candidatsNoyau.toString() +
                    ".<br>La destination recommandée (flux positif max) est : <b>" + meilleurNom + "</b>.";
        }
    }

    private void afficherMatrices(double[][] conc, double[][] disc, String surclassement, double seuilC, double seuilD) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = new JFrame("Résultats ÉLECTRE I (C ≥ " + seuilC + " | D ≤ " + seuilD + ")");
            fenetre.setSize(1600, 850);
            fenetre.setLocationRelativeTo(frame);

            GradientPanel mainPanel = new GradientPanel();
            mainPanel.setLayout(new BorderLayout());
            fenetre.add(mainPanel);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(Style.BOLD_FONT);
            tabbedPane.setOpaque(false);
            mainPanel.add(tabbedPane, BorderLayout.CENTER);

            int n = extraireDonneesTableau().length;
            String[] alternativeNames = new String[n];
            for (int i = 0; i < n; i++) alternativeNames[i] = getAlternativeName(i);

            
            JPanel matricesPanel = new JPanel(new GridLayout(1, 3, 15, 15));
            matricesPanel.setOpaque(false);
            matricesPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            matricesPanel.add(creerTableau("Matrice de Concordance", conc, alternativeNames, Style.PRIMARY_COLOR));
            matricesPanel.add(creerTableau("Matrice de Discordance", disc, alternativeNames, Style.DANGER_COLOR));
            matricesPanel.add(creerTableauSurclassement(conc, disc, alternativeNames, seuilC, seuilD));
            tabbedPane.addTab("Matrices de Calcul", matricesPanel);

           
            boolean[][] surclassementBinaire = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    surclassementBinaire[i][j] = (i != j) && (conc[i][j] >= seuilC && disc[i][j] <= seuilD);
                }
            }
            GraphPanel graphPanel = new GraphPanel(surclassementBinaire, alternativeNames);
            tabbedPane.addTab("Graphe de Surclassement", graphPanel);

            
            JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 15, 15));
            summaryPanel.setOpaque(false);
            summaryPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JTextArea surclasseText = new JTextArea(surclassement);
            surclasseText.setEditable(false);
            surclasseText.setFont(new Font("Monospaced", Font.PLAIN, 15));
            surclasseText.setForeground(Style.TEXT_COLOR);
            surclasseText.setLineWrap(true);
            surclasseText.setWrapStyleWord(true);
            surclasseText.setOpaque(false);

            JScrollPane scroll = new JScrollPane(surclasseText);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(createTitledBorder("Synthèse des Surclassements", Style.PRIMARY_COLOR));
            summaryPanel.add(scroll);

            String meilleures = trouverMeilleuresAlternatives(conc, disc, seuilC, seuilD);
            JEditorPane meilleurText = new JEditorPane("text/html",
                    "<html><body style='font-family: Inter; text-align: center; background-color: transparent;'>" +
                            "<h2 style='color: " + toHex(Style.PRIMARY_COLOR) + ";'>Recommandation Finale</h2>" +
                            "<p style='font-size: 14pt; color: " + toHex(Style.TEXT_COLOR) + ";'>" + meilleures + "</p>" +
                            "</body></html>"
            );
            meilleurText.setEditable(false);
            meilleurText.setOpaque(false);

            JPanel meilleurPanel = new JPanel(new BorderLayout());
            meilleurPanel.setOpaque(false);
            meilleurPanel.add(meilleurText, BorderLayout.CENTER);
            meilleurPanel.setBorder(createTitledBorder("Meilleure Alternative", Style.SUCCESS_COLOR));
            summaryPanel.add(meilleurPanel);
            tabbedPane.addTab("Synthèse & Recommandation", summaryPanel);

            fenetre.setVisible(true);
        });
    }

    private JScrollPane creerTableau(String titre, double[][] data, String[] noms, Color accentColor) {
        DefaultTableModel dm = new DefaultTableModel();
        dm.addColumn("");
        for (String nom : noms) dm.addColumn(nom);

        for (int i = 0; i < data.length; i++) {
            Object[] row = new Object[data[i].length + 1];
            row[0] = noms[i];
            for (int j = 0; j < data[i].length; j++) {
                row[j + 1] = String.format("%.3f", data[i][j]);
            }
            dm.addRow(row);
        }

        JTable t = new JTable(dm);
        t.setRowHeight(35);
        t.setFont(Style.MAIN_FONT);
        t.setOpaque(false);
        ((JComponent) t.getDefaultRenderer(Object.class)).setOpaque(false);

        JTableHeader header = t.getTableHeader();
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(accentColor);
        header.setFont(Style.BOLD_FONT);

        ResultMatrixRenderer renderer = new ResultMatrixRenderer(titre.contains("Concordance"));
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scroll = new JScrollPane(t);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(createTitledBorder(titre, accentColor));
        return scroll;
    }

    private JScrollPane creerTableauSurclassement(double[][] conc, double[][] disc, String[] noms, double seuilC, double seuilD) {
        DefaultTableModel dm = new DefaultTableModel();
        dm.addColumn("");
        for (String nom : noms) dm.addColumn(nom);

        int n = conc.length;
        for (int i = 0; i < n; i++) {
            Object[] row = new Object[n + 1];
            row[0] = noms[i];
            for (int j = 0; j < n; j++) {
                if (i == j) row[j + 1] = "-";
                else row[j + 1] = (conc[i][j] >= seuilC && disc[i][j] <= seuilD) ? "✔" : "";
            }
            dm.addRow(row);
        }

        JTable t = new JTable(dm);
        t.setRowHeight(35);
        t.setOpaque(false);
        ((JComponent) t.getDefaultRenderer(Object.class)).setOpaque(false);

        JTableHeader header = t.getTableHeader();
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(Style.PRIMARY_COLOR);
        header.setFont(Style.BOLD_FONT);

        SurclassementMatrixRenderer renderer = new SurclassementMatrixRenderer();
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scroll = new JScrollPane(t);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(createTitledBorder("Matrice de Surclassement", Style.PRIMARY_COLOR));
        return scroll;
    }

    private Border createTitledBorder(String title, Color color) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(color, 1, true),
                title,
                TitledBorder.LEFT, TitledBorder.TOP,
                Style.BOLD_FONT, color
        );
    }

    

    private class AlternatingRowRendererContrast extends DefaultTableCellRenderer {
        public AlternatingRowRendererContrast() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Style.BORDER_COLOR),
                    new EmptyBorder(10, 15, 10, 15)
            ));
        }

        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                setBackground(Style.SELECTION_COLOR);
                setForeground(Style.TEXT_COLOR);
            } else {
                if (Objects.equals(model.getValueAt(row, 0), POIDS_ROW_NAME)) {
                    setBackground(Style.WEIGHT_ROW_BACKGROUND);
                    setForeground(Style.WEIGHT_ROW_FOREGROUND);
                    setFont(Style.BOLD_FONT);
                } else {
                    setBackground(Style.PANEL_BACKGROUND);
                    setForeground(Style.TEXT_COLOR);
                    setFont(Style.MAIN_FONT);
                }
            }
            setHorizontalAlignment(column > 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
            return this;
        }
    }

    private class ResultMatrixRenderer extends DefaultTableCellRenderer {
        private final boolean isConcordance;
        public ResultMatrixRenderer(boolean isConcordance) {
            this.isConcordance = isConcordance;
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setOpaque(false);
        }

       
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0) {
                setFont(Style.BOLD_FONT);
                setForeground(Style.TEXT_COLOR);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBackground(new Color(0,0,0,0));
                return this;
            }

            if (row == column - 1) {
                setBackground(new Color(230, 230, 230));
                setForeground(Color.GRAY);
                return this;
            }

            try {
                double val = Double.parseDouble(value.toString().replace(',', '.'));
                Color lowColor = new Color(245, 249, 255);
                Color highColor = isConcordance ? new Color(0, 150, 136, 100) : new Color(244, 67, 54, 100);
                float ratio = (float) (isConcordance ? val : 1.0 - val);
                int red = (int) (lowColor.getRed() + ratio * (highColor.getRed() - lowColor.getRed()));
                int green = (int) (lowColor.getGreen() + ratio * (highColor.getGreen() - lowColor.getGreen()));
                int blue = (int) (lowColor.getBlue() + ratio * (highColor.getBlue() - lowColor.getBlue()));
                setBackground(new Color(red, green, blue));
                setForeground(Style.TEXT_COLOR);
            } catch (NumberFormatException e) {
                setBackground(new Color(0,0,0,0));
            }
            return this;
        }
    }

    private class SurclassementMatrixRenderer extends DefaultTableCellRenderer {
        public SurclassementMatrixRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setOpaque(false);
        }

        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0) {
                setFont(Style.BOLD_FONT);
                setForeground(Style.TEXT_COLOR);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBackground(new Color(0,0,0,0));
                return this;
            }

            setFont(getFont().deriveFont(Font.BOLD, 18f));
            if ("✔".equals(value)) {
                setForeground(Style.SUCCESS_COLOR);
                setBackground(new Color(232, 245, 233));
            } else if ("-".equals(value)) {
                setForeground(Color.LIGHT_GRAY);
                setBackground(new Color(230, 230, 230));
            } else {
                setForeground(Color.DARK_GRAY);
                setBackground(new Color(0,0,0,0));
            }
            return this;
        }
    }

    class GraphPanel extends JPanel {
        private final boolean[][] outrankingMatrix;
        private final String[] alternativeNames;
        private final Point2D[] nodePositions;
        private final int NODE_DIAMETER = 60;

        public GraphPanel(boolean[][] matrix, String[] names) {
            this.outrankingMatrix = matrix;
            this.alternativeNames = names;
            this.nodePositions = new Point2D[names.length];
            setOpaque(false);
        }

        private void calculateNodePositions() {
            int n = alternativeNames.length;
            if (n == 0) return;

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int radius = Math.min(centerX, centerY) - NODE_DIAMETER;

            for (int i = 0; i < n; i++) {
                double angle = 2 * Math.PI * i / n - Math.PI / 2;
                int x = centerX + (int) (radius * Math.cos(angle));
                int y = centerY + (int) (radius * Math.sin(angle));
                nodePositions[i] = new Point2D.Double(x, y);
            }
        }

        
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            calculateNodePositions();

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Style.TEXT_COLOR.darker());
            for (int i = 0; i < outrankingMatrix.length; i++) {
                for (int j = 0; j < outrankingMatrix[i].length; j++) {
                    if (outrankingMatrix[i][j]) {
                        drawArrow(g2, nodePositions[i], nodePositions[j]);
                    }
                }
            }

            for (int i = 0; i < alternativeNames.length; i++) {
                int x = (int) (nodePositions[i].getX() - NODE_DIAMETER / 2);
                int y = (int) (nodePositions[i].getY() - NODE_DIAMETER / 2);

                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillOval(x + 3, y + 3, NODE_DIAMETER, NODE_DIAMETER);

                g2.setColor(Style.PRIMARY_COLOR);
                g2.fillOval(x, y, NODE_DIAMETER, NODE_DIAMETER);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(x, y, NODE_DIAMETER, NODE_DIAMETER);

                g2.setFont(Style.BOLD_FONT);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String name = alternativeNames[i];
                if (fm.stringWidth(name) > NODE_DIAMETER - 10) {
                    name = name.substring(0, Math.min(name.length(), 5)) + "...";
                }
                int textWidth = fm.stringWidth(name);
                g2.drawString(name, x + (NODE_DIAMETER - textWidth) / 2, y + (NODE_DIAMETER - fm.getHeight()) / 2 + fm.getAscent());
            }

            g2.dispose();
        }

        private void drawArrow(Graphics2D g2, Point2D from, Point2D to) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double angle = Math.atan2(dy, dx);

            double startOffset = NODE_DIAMETER / 2.0;
            double endOffset = NODE_DIAMETER / 2.0 + 10;

            Point2D.Double p1 = new Point2D.Double(from.getX() + startOffset * Math.cos(angle), from.getY() + startOffset * Math.sin(angle));
            Point2D.Double p2 = new Point2D.Double(to.getX() - endOffset * Math.cos(angle), to.getY() - endOffset * Math.sin(angle));

            g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

            AffineTransform tx = g2.getTransform();
            Polygon arrowHead = new Polygon();
            arrowHead.addPoint(0, 5);
            arrowHead.addPoint(-5, -5);
            arrowHead.addPoint(5, -5);

            tx.translate(to.getX() - (NODE_DIAMETER/2.0) * Math.cos(angle), to.getY() - (NODE_DIAMETER/2.0) * Math.sin(angle));
            tx.rotate(angle - Math.PI / 2d);

            g2.setTransform(tx);
            g2.fill(arrowHead);
            g2.setTransform(new AffineTransform());
        }
    }

   
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
