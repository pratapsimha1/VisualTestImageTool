
package com.visualTest.Nm;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;


public class VisualTestTool extends JFrame {
    private JLabel expectedLabel;
    private JLabel actualLabel;
    private JLabel resultLabel;
    private JCheckBox darkModeToggle;
    private JPanel buttonPanel;
    private JButton toggleButtonsButton;

    private File expectedFile;
    private File actualFile;

    public VisualTestTool() {
        setTitle("Visual Comparison Tool");
        setSize(1000, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Visual Test Comparison Tool", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        darkModeToggle = new JCheckBox("Dark Mode");
        buttonPanel.add(darkModeToggle);

        JButton selectExpectedButton = new JButton("Select Expected Image");
        JButton selectActualButton = new JButton("Select Actual Image");
        JButton compareButton = new JButton("Compare Images");
        JButton exportCSVButton = new JButton("Export CSV");
        JButton exportPDFButton = new JButton("Export PDF");
        toggleButtonsButton = new JButton("Toggle Panel");

        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(selectExpectedButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(selectActualButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(compareButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exportCSVButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exportPDFButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(toggleButtonsButton);

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.add(buttonPanel, BorderLayout.CENTER);
        add(buttonWrapper, BorderLayout.WEST);

        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        expectedLabel = new JLabel();
        actualLabel = new JLabel();
        resultLabel = new JLabel();

        JPanel expectedContainer = wrapImageLabel(expectedLabel, "Expected Image");
        JPanel actualContainer = wrapImageLabel(actualLabel, "Actual Image");
        JPanel resultContainer = wrapImageLabel(resultLabel, "Result Image");

        imagePanel.add(expectedContainer);
        imagePanel.add(Box.createVerticalStrut(20));
        imagePanel.add(actualContainer);
        imagePanel.add(Box.createVerticalStrut(20));
        imagePanel.add(resultContainer);

        JScrollPane scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);

        darkModeToggle.addActionListener(e -> toggleDarkMode());
        toggleButtonsButton.addActionListener(e -> buttonPanel.setVisible(!buttonPanel.isVisible()));
        selectExpectedButton.addActionListener(e -> chooseImage(true));
        selectActualButton.addActionListener(e -> chooseImage(false));

        compareButton.addActionListener(e -> {
            try {
                compareImages();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        exportCSVButton.addActionListener(e -> exportCSV("comparison_summary.csv"));
        exportPDFButton.addActionListener(e -> exportPDF("comparison_report.pdf"));
    }

    private JPanel wrapImageLabel(JLabel label, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        panel.add(new JScrollPane(label), BorderLayout.CENTER);
        return panel;
    }

    private void toggleDarkMode() {
        boolean dark = darkModeToggle.isSelected();
        Color bg = dark ? Color.DARK_GRAY : Color.LIGHT_GRAY;
        Color fg = dark ? Color.WHITE : Color.BLACK;
        getContentPane().setBackground(bg);
        for (Component c : getContentPane().getComponents()) {
            c.setBackground(bg);
            c.setForeground(fg);
        }
    }

    private void compareImages() throws IOException {
        BufferedImage expected = ImageIO.read(expectedFile);
        BufferedImage actual = ImageIO.read(actualFile);
        int w = Math.max(expected.getWidth(), actual.getWidth());
        int h = Math.max(expected.getHeight(), actual.getHeight());
        BufferedImage paddedExpected = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage paddedActual = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = paddedExpected.createGraphics();
        g1.drawImage(expected, 0, 0, null);
        g1.dispose();
        Graphics2D g2 = paddedActual.createGraphics();
        g2.drawImage(actual, 0, 0, null);
        g2.dispose();
        expected = paddedExpected;
        actual = paddedActual;

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(actual, 0, 0, null);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        boolean[][] visited = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!visited[x][y] && expected.getRGB(x, y) != actual.getRGB(x, y)) {
                    Rectangle r = floodFill(expected, actual, visited, x, y);
                    g.drawRect(r.x, r.y, r.width, r.height);
                }
            }
        }
        g.dispose();
        ImageIO.write(result, "png", new File("result.png"));
        resultLabel.setIcon(new ImageIcon("result.png"));
    }

    private Rectangle floodFill(BufferedImage img1, BufferedImage img2, boolean[][] visited, int x, int y) {
        int minX = x, minY = y, maxX = x, maxY = y;
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            if (p.x < 0 || p.y < 0 || p.x >= visited.length || p.y >= visited[0].length || visited[p.x][p.y]) continue;
            if (img1.getRGB(p.x, p.y) != img2.getRGB(p.x, p.y)) {
                visited[p.x][p.y] = true;
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
                stack.push(new Point(p.x + 1, p.y));
                stack.push(new Point(p.x - 1, p.y));
                stack.push(new Point(p.x, p.y + 1));
                stack.push(new Point(p.x, p.y - 1));
            }
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void chooseImage(boolean isExpected) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            JLabel targetLabel = isExpected ? expectedLabel : actualLabel;
            ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
            targetLabel.setIcon(imageIcon);
            targetLabel.setPreferredSize(new Dimension(imageIcon.getIconWidth(), imageIcon.getIconHeight()));
            targetLabel.revalidate();
            if (isExpected) {
                expectedFile = selectedFile;
            } else {
                actualFile = selectedFile;
            }
        }
    }

    private void exportCSV(String filename) {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println("Region,X,Y,Width,Height");
            out.println("Example,10,20,30,40");
            JOptionPane.showMessageDialog(this, "CSV exported: " + filename);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to export CSV");
        }
    }

    private String extractTextFromImage(File imageFile) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata"); // Adjust for your system
        try {
            return tesseract.doOCR(imageFile);
        } catch (TesseractException e) {
            return "OCR Error: " + e.getMessage();
        }
    }

    private void exportPDF(String filename) {
        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(filename));
            doc.open();
            doc.add(new Paragraph("Visual Comparison Report"));
            doc.add(new Paragraph("Generated: " + new Date()));
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance("result.png");
            img.scaleToFit(500, 400);
            doc.add(img);
            String ocrText = extractTextFromImage(new File("result.png"));
            doc.add(new Paragraph("OCR-detected differences:"));
            doc.add(new Paragraph(ocrText));
            doc.close();
            JOptionPane.showMessageDialog(this, "PDF exported: " + filename);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to export PDF: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VisualTestTool().setVisible(true));
    }
}
