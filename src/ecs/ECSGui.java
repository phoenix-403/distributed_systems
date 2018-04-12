package ecs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.*;

public class ECSGui extends Frame implements WindowListener, ActionListener{

    int width;
    int height;
    JButton refreshButton;
    JPanel topPanel;
    int count = 0;
    ArrayList<Node> nodes;
    ArrayList<edge> edges;
    int currentServerCount;
    int nodesCount;

    public ECSGui(String title){
        super(title);
        setSize(2000, 700);
        currentServerCount = 0;
        nodesCount = 0;
        nodes = new ArrayList<Node>();
        edges = new ArrayList<edge>();

    }

    public void initialize(){
        addWindowListener(this);
//        topPanel = new JPanel();
//        this.add(topPanel, BorderLayout.NORTH);
//        topPanel.setBackground(Color.ORANGE);
//
//        refreshButton = new JButton("Refresh");
//        refreshButton.addActionListener(this);
//        topPanel.add(refreshButton);
    }

    class Node {
        int x, y;
        String name;
        Color color;

        public Node(String myName, int myX, int myY, Color myColor) {
            x = myX;
            y = myY;
            name = myName;
            color = myColor;
        }
    }

    class edge {
        int i,j;

        public edge(int ii, int jj) {
            i = ii;
            j = jj;
        }
    }

    public void addNode(String name, int x, int y, Color color) {
        //add a node at pixel (x,y)
        nodes.add(new Node(name,x,y, color));
        this.repaint();
    }
    public void addEdge(int i, int j) {
        //add an edge between nodes i and j
        edges.add(new edge(i,j));
        this.repaint();
    }

    public void paint(Graphics g) { // draw the nodes and edges
        FontMetrics f = g.getFontMetrics();
        int nodeHeight = Math.max(height, f.getHeight());
        nodeHeight = nodeHeight + 10;

        g.setColor(Color.black);
        for (edge e : edges) {
            g.drawLine(nodes.get(e.i).x, nodes.get(e.i).y,
                    nodes.get(e.j).x, nodes.get(e.j).y);
        }

        for (Node n : nodes) {
            int nodeWidth = Math.max(width, f.stringWidth(n.name)+width/2);
            nodeWidth = nodeWidth + 10;
            g.setColor(n.color);
            g.fillOval(n.x-nodeWidth/2, n.y-nodeHeight/2,
                    nodeWidth, nodeHeight);
            g.setColor(Color.black);
            g.drawOval(n.x-nodeWidth/2, n.y-nodeHeight/2,
                    nodeWidth, nodeHeight);

            g.drawString(n.name, n.x-f.stringWidth(n.name)/2,
                    n.y+f.getHeight()/2);
        }
    }


    public void refresh(){
        //clear screen
        getGraphics().clearRect(0,0, getWidth(), getHeight());

        //perform the redraw action here (i.e. call drawServerNode)
    }


    public void actionPerformed(ActionEvent e){
    }

    public void windowClosing(WindowEvent e){
        dispose();
    }

    public void drawServerNode(String serverName,ArrayList<String> serverClients){
        this.currentServerCount++;

        // server stuff
        int sXpos = this.getWidth() - (this.currentServerCount * 550);
        int sYpos = this.getHeight()/2;
        this.addNode(serverName, sXpos, sYpos, Color.RED);
        int serverNodeCount = nodesCount;
        nodesCount++;

        if(serverClients != null) {
            // client stuff
            int radius = 200;
            float position = 0;
            float increments = 360/serverClients.size();
            System.out.println(increments);

            for (String client : serverClients) {
                int clientX = (int)(radius * Math.cos(Math.toRadians(position))) + sXpos;
                int clientY = (int)(radius * Math.sin(Math.toRadians(position))) + sYpos;
                this.addNode(client, clientX, clientY, Color.ORANGE);
                this.addEdge(serverNodeCount, nodesCount);
                nodesCount++;
                position += increments;
            }
        }

    }


    public static void main(String[] args){
        ECSGui ecsGui = new ECSGui("ECS Client GUI");
        ecsGui.initialize();
        ecsGui.setVisible(true);

        // to refresh
        ecsGui.refresh();
        // draw again

//        ArrayList<String> a = new ArrayList<String>(Arrays.asList("localhost:404040", "localhost:404040", "localhost:404040", "localhost:404040"));
//        ArrayList<String> b = new ArrayList<String>(Arrays.asList("localhost:404040", "localhost:404040", "localhost:404040"));
//        ecsGui.drawServerNode("localhost:404040",a);
//        ecsGui.drawServerNode("localhost:404040",b);
//        ecsGui.drawServerNode("localhost:404040",null);
//
    }

    public void windowOpened(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}



}
