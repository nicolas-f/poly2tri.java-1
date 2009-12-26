package org.poly2tri.examples.ardor3d;

import java.io.IOException;
import java.nio.FloatBuffer;

import org.poly2tri.examples.ardor3d.misc.PolygonLoader;
import org.poly2tri.examples.ardor3d.misc.Triangle;
import org.poly2tri.polygon.Polygon;
import org.poly2tri.triangulation.TriangulationContext;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.delaunay.sweep.AdvancingFront;
import org.poly2tri.triangulation.delaunay.sweep.AdvancingFrontNode;
import org.poly2tri.triangulation.delaunay.sweep.DTSweepConstraint;
import org.poly2tri.triangulation.delaunay.sweep.DTSweepContext;
import org.poly2tri.triangulation.sets.PolygonSet;
import org.poly2tri.triangulation.util.PolygonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.BufferUtils;
import com.google.inject.Inject;

/**
 * Toggle Model with PageUp and PageDown<br>
 * Toggle Wireframe with Home<br>
 * Toggle Vertex points with End<br>
 * Use 1 and 2 to generate random polygons<br>
 * 
 * @author Thomas
 *
 */
public class CDTModelExample extends CDTExampleBase
{
    private final static Logger logger = LoggerFactory.getLogger( CDTModelExample.class );
    
    private static double SCALE = 50;
        
    private Line m_line;
    
    // Build parameters
    private int m_vertexCount = 10000;
    
    // Scene components
    private CDTSweepAdvancingFront _cdtSweepAdvancingFront;
    private CDTSweepActiveNode _cdtSweepActiveNode;
    private CDTSweepActiveTriangles m_cdtSweepActiveTriangle;
    private CDTSweepActiveEdge m_cdtSweepActiveEdge;
//    private GUICircumCircle m_circumCircle;
        
    private int m_stepCount = 0;
    private boolean m_autoStep = true;

    private final String m_dataPath = "src/main/resources/org/poly2tri/examples/data/";
    
    public static void main(final String[] args) 
    {
        start(CDTModelExample.class);
    }

    @Inject
    public CDTModelExample( LogicalLayer logicalLayer, FrameHandler frameHandler )
    {
        super( logicalLayer, frameHandler );
    }

    protected void updateExample(final ReadOnlyTimer timer) 
    {
        super.updateExample( timer );
        
        if( _process.getContext().isDebugEnabled() )
        {
            int count = _process.getStepCount();
            if( m_stepCount < count )
            {
                _process.requestRead();
                if( _process.isReadable() )
                {
                    updateMesh();
                    m_stepCount = count;
                    if( m_autoStep )
                    {
                        _process.resume();                    
                    }
                }
            }
        }
    }

    @Override
    protected void initExample()
    {
        super.initExample();
        
        if( _process.getContext().isDebugEnabled() )
        {    
            _cdtSweepAdvancingFront = new CDTSweepAdvancingFront();
            _node.attachChild( _cdtSweepAdvancingFront.getSceneNode() );
            
            _cdtSweepActiveNode = new CDTSweepActiveNode();
            _node.attachChild( _cdtSweepActiveNode.getSceneNode() );
    
            m_cdtSweepActiveTriangle = new CDTSweepActiveTriangles();
            _node.attachChild( m_cdtSweepActiveTriangle.getSceneNode() );
            
            m_cdtSweepActiveEdge = new CDTSweepActiveEdge();
            _node.attachChild( m_cdtSweepActiveEdge.getSceneNode() );

//          m_circumCircle = new GUICircumCircle();
//          m_node.attachChild( m_circumCircle.getSceneNode() );
        }
        
        buildModel(m_currentModel); 
        triangulate();
    }
    
    /**
     * Update text information.
     */
    protected void updateText() 
    {
        super.updateText();
        _exampleInfo[3].setText("[PageUp] Next model");
        _exampleInfo[4].setText("[PageDown] Previous model");
        _exampleInfo[5].setText("[1] Generate polygon type A ");
        _exampleInfo[6].setText("[2] Generate polygon type B ");
    }

    private void buildModel( ExampleModels model )
    {
        Polygon poly;
        if( model != null )
        {
            try
            {
                poly = PolygonLoader.loadModel( model, SCALE );
                _polygonSet = new PolygonSet( poly );        
            }
            catch( IOException e )
            {
                logger.info( "Failed to load model {}", e.getMessage() );
                model = null;
            }
        }
        
        if( model == null )
        {
            _polygonSet = new PolygonSet( PolygonGenerator.RandomCircleSweep( SCALE, m_vertexCount ) );        
        }                
    }
        
    protected void triangulate()
    {
        super.triangulate();
        m_stepCount = 0;
    }

    protected void updateMesh()
    {        
        super.updateMesh();
        
        TriangulationContext tcx = _process.getContext();
        
        if( tcx.isDebugEnabled() )
        {
            m_cdtSweepActiveTriangle.update( tcx );
            m_cdtSweepActiveEdge.update( tcx );
            _cdtSweepActiveNode.update( tcx );
            _cdtSweepAdvancingFront.update( tcx );            
//          m_circumCircle.update( tcx.getCircumCircle() );
        }
    }
    


    @Override
    public void registerInputTriggers()
    {
        super.registerInputTriggers();
        
        // SPACE - toggle models
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.PAGEUP_PRIOR ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                int index;
                index = (m_currentModel.ordinal()+1)%ExampleModels.values().length;
                m_currentModel = ExampleModels.values()[index];
                buildModel(m_currentModel); 
                _node.setScale( m_currentModel.getScale() );
                triangulate();
            }
        } ) );  
        
        // SPACE - toggle models backwards
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.PAGEDOWN_NEXT ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                int index;
                index = ((m_currentModel.ordinal()-1)%ExampleModels.values().length + ExampleModels.values().length)%ExampleModels.values().length;
                m_currentModel = ExampleModels.values()[index];
                buildModel(m_currentModel); 
                _node.setScale( m_currentModel.getScale() );
                triangulate();
            }
        } ) );  

        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.ONE ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                _polygonSet = new PolygonSet( PolygonGenerator.RandomCircleSweep( SCALE, m_vertexCount ) );
                triangulate();
            }
        } ) );          

        
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.TWO ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                _polygonSet = new PolygonSet( PolygonGenerator.RandomCircleSweep2( SCALE, 200 ) );
                triangulate();
            }
        } ) );          

        // X -start
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.X ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                // Lets create a TriangulationProcess that allows you to step thru the TriangulationAlgorithm        
//                m_process.getContext().isDebugEnabled( true );
//                _process.triangulate();
//                m_stepCount = 0;
            }
        } ) );          

        // C - step
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.C ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                _process.resume();
            }
        } ) );      
        
        // Z - toggle autostep
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.Z ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                m_autoStep = m_autoStep ? false : true;
            }
        } ) );                  

        // space - save triangle lines
        _logicalLayer.registerTrigger( new InputTrigger( new KeyPressedCondition( Key.SPACE ), new TriggerAction() {
            public void perform( final Canvas canvas, final TwoInputStates inputState, final double tpf )
            {
                PolygonLoader.saveTriLine( m_dataPath, _polygonSet ); 
            }
        } ) );                  
    }
    
    class CDTSweepAdvancingFront extends SceneElement<TriangulationContext>
    {
        protected Line m_nodeLines;
        protected Point m_frontPoints;
        protected Line m_frontLine;
        
        public CDTSweepAdvancingFront()
        {
            super("AdvancingFront");
            m_frontLine = new Line();
            m_frontLine.getMeshData().setIndexMode( IndexMode.LineStrip );
            m_frontLine.getMeshData().setVertexBuffer( BufferUtils.createVector3Buffer( 800 ) );
            m_frontLine.setDefaultColor( ColorRGBA.ORANGE );
            m_frontLine.setTranslation( 0, 0.05, 0 );
            m_node.attachChild( m_frontLine );
            
            m_frontPoints = new Point();
            m_frontPoints.getMeshData().setVertexBuffer( m_frontLine.getMeshData().getVertexBuffer() );
            m_frontPoints.setPointSize( 6 );
            m_frontPoints.setDefaultColor( ColorRGBA.ORANGE );
            m_frontPoints.setTranslation( 0, 0.05, 0 );
            m_node.attachChild( m_frontPoints );
            
            m_nodeLines = new Line();
            m_nodeLines.getMeshData().setIndexMode( IndexMode.Lines );
            m_nodeLines.getMeshData().setVertexBuffer( BufferUtils.createVector3Buffer( 2*800 ) );
            m_nodeLines.setDefaultColor( ColorRGBA.YELLOW );
            m_nodeLines.setTranslation( 0, 0.05, 0 );
            m_node.attachChild( m_nodeLines );
        }
        
        @Override
        public void update( TriangulationContext tcx  )
        {
            AdvancingFront front = ((DTSweepContext)tcx).getAdvancingFront();
            AdvancingFrontNode node;
            DelaunayTriangle tri;

            if( front == null ) return;
            FloatBuffer fb = m_frontLine.getMeshData().getVertexBuffer();
            FloatBuffer nodeVert = m_nodeLines.getMeshData().getVertexBuffer();
//            if( fb.capacity() < front.nodeCount*3 )
//            {
//                // Make it twice the size so we got something to grow with
//                fb = BufferUtils.createFloatBuffer( 2*front.nodeCount*3 );
//                m_frontLine.getMeshData().setVertexBuffer(fb);
//            }
//            else
//            {
//                fb.limit( front.nodeCount*3 );
//            }
            fb.limit( 800*3 );
            nodeVert.limit( 2*800*3 );
            fb.rewind();
            nodeVert.rewind(); 
            
            int count=0;
            node = front.head;
            TriangulationPoint point;
            do
            {
                point = node.getPoint();
                fb.put( point.getXf() ).put( point.getYf() ).put( point.getZf() );
                tri = node.getTriangle();
                if( tri != null )
                {
                    nodeVert.put( point.getXf() ).put( point.getYf() ).put( point.getZf() );
                    nodeVert.put( ( tri.points[0].getXf() + tri.points[1].getXf() + tri.points[2].getXf() )/3 );
                    nodeVert.put( ( tri.points[0].getYf() + tri.points[1].getYf() + tri.points[2].getYf() )/3 );
                    nodeVert.put( ( tri.points[0].getZf() + tri.points[1].getZf() + tri.points[2].getZf() )/3 );
                }
                count++;
            } while( (node = node.getNext()) != null );
            fb.limit( 3*count );
            nodeVert.limit( 2*count*3 ); 
        }
    }
    
    
//    class GUICircumCircle extends SceneElement<Tuple2<TriangulationPoint,Double>>
//    {
//        private int VCNT = 64;
//        private Line m_circle = new Line();
//        
//        public GUICircumCircle()
//        {
//            super("CircumCircle");
//            m_circle.getMeshData().setIndexMode( IndexMode.LineLoop );
//            m_circle.getMeshData().setVertexBuffer( BufferUtils.createVector3Buffer( VCNT ) );
//            m_circle.setDefaultColor( ColorRGBA.WHITE );
//            m_circle.setLineWidth( 1 );
//            m_node.attachChild( m_circle );
//        }        
//        
//        @Override
//        public void update( Tuple2<TriangulationPoint,Double> circle )
//        {
//            float x,y;
//            if( circle.a != null )
//            {
//                FloatBuffer fb = m_circle.getMeshData().getVertexBuffer();
//                fb.rewind();
//                for( int i=0; i < VCNT; i++ )
//                {
//                    x = (float)circle.a.getX() + (float)(circle.b*Math.cos( 2*Math.PI*((double)i%VCNT)/VCNT ));
//                    y = (float)circle.a.getY() + (float)(circle.b*Math.sin( 2*Math.PI*((double)i%VCNT)/VCNT ));
//                    fb.put( x ).put( y ).put( 0 );
//                }
//            }
//            else
//            {
//                m_node.detachAllChildren();
//            }
//        }
//    }

    class CDTSweepMeshExtended extends CDTSweepMesh
    {
//        private Line m_conLine = new Line();
        
        public CDTSweepMeshExtended()
        {
            super();
            
            // Line that show the connection between triangles
//            m_conLine.setDefaultColor( ColorRGBA.RED );
//            m_conLine.getMeshData().setIndexMode( IndexMode.Lines );
//            m_node.attachChild( m_conLine );
//            
//            vertBuf = BufferUtils.createFloatBuffer( size*3*3*3 );
//            m_conLine.getMeshData().setVertexBuffer( vertBuf );
        }        
                
        @Override
        public void update( PolygonSet ps )
        {
            super.update( ps );
//            MeshData md;
//            Vector3 v1 = Vector3.fetchTempInstance();
//            Vector3 v2 = Vector3.fetchTempInstance();
//            FloatBuffer v2Buf;
//
//
//            md = m_mesh.getMeshData();
//            v2Buf = m_conLine.getMeshData().getVertexBuffer();
//
////            logger.info( "Triangle count [{}]", tcx.getMap().size() );
//            
//            int size = 2*3*3*ps.getTriangles().size();
//            if( v2Buf.capacity() < size )
//            {
//                v2Buf = BufferUtils.createFloatBuffer( size );
//                m_conLine.getMeshData().setVertexBuffer( v2Buf );
//            }
//            else
//            {
//                v2Buf.limit( 2*size );
//            }
//
//            v2Buf.rewind();
//            int lineCount=0;
//            ArdorVector3Point p;
//            for( DelaunayTriangle t : ps.getTriangles() )
//            {
//                v1.set( t.points[0] ).addLocal( t.points[1] ).addLocal( t.points[2] ).multiplyLocal( 1.0d/3 );
//                if( t.neighbors[0] != null )
//                {
//                    v2.set( t.points[2] ).subtractLocal( t.points[1] ).multiplyLocal( 0.5 ).addLocal( t.points[1] );
//                    v2Buf.put( v1.getXf() ).put( v1.getYf() ).put( v1.getZf() );
//                    v2Buf.put( v2.getXf() ).put( v2.getYf() ).put( v2.getZf() );
//                    lineCount++;
//                }
//                if( t.neighbors[1] != null )
//                {
//                    v2.set( t.points[0] ).subtractLocal( t.points[2] ).multiplyLocal( 0.5 ).addLocal( t.points[2] );
//                    v2Buf.put( v1.getXf() ).put( v1.getYf() ).put( v1.getZf() );
//                    v2Buf.put( v2.getXf() ).put( v2.getYf() ).put( v2.getZf() );
//                    lineCount++;
//                }
//                if( t.neighbors[2] != null )
//                {
//                    v2.set( t.points[1] ).subtractLocal( t.points[0] ).multiplyLocal( 0.5 ).addLocal( t.points[0] );
//                    v2Buf.put( v1.getXf() ).put( v1.getYf() ).put( v1.getZf() );
//                    v2Buf.put( v2.getXf() ).put( v2.getYf() ).put( v2.getZf() );
//                    lineCount++;
//                }
//            }
//            v2Buf.limit( 2*3*lineCount );
//            Vector3.releaseTempInstance( v1 );
//            Vector3.releaseTempInstance( v2 );
        }
    }

    class CDTSweepActiveEdge extends SceneElement<TriangulationContext>
    {
        private Line m_edgeLine = new Line();
        
        public CDTSweepActiveEdge()
        {
            super("ActiveEdge");
            m_edgeLine.getMeshData().setIndexMode( IndexMode.Lines );
            m_edgeLine.getMeshData().setVertexBuffer( BufferUtils.createVector3Buffer( 2 ) );
            m_edgeLine.setDefaultColor( ColorRGBA.YELLOW );
            m_edgeLine.setLineWidth( 3 );
        }        
        
        @Override
        public void update( TriangulationContext tcx )
        {
            DTSweepConstraint edge = tcx.getDebugContext().getActiveConstraint();
            if( edge != null )
            {
                FloatBuffer fb = m_edgeLine.getMeshData().getVertexBuffer();
                fb.rewind();
                fb.put( edge.getP().getXf() ).put( edge.getP().getYf() ).put( 0 );
                fb.put( edge.getQ().getXf() ).put( edge.getQ().getYf() ).put( 0 );
                m_node.attachChild( m_edgeLine );
            }
            else
            {
                m_node.detachAllChildren();
            }
        }
    }
    
    class CDTSweepActiveTriangles extends SceneElement<TriangulationContext>
    {
        private Triangle m_a = new Triangle();
        private Triangle m_b = new Triangle();
        
        public CDTSweepActiveTriangles()
        {
            super("ActiveTriangles");
            m_node.getSceneHints().setAllPickingHints( false );
            m_a.setDefaultColor( new ColorRGBA( 0.8f,0.8f,0.8f,1.0f ) );
            m_b.setDefaultColor( new ColorRGBA( 0.5f,0.5f,0.5f,1.0f ) );
        }

        public void setScale( double scale )
        {
            m_a.setScale( scale );
            m_b.setScale( scale );
        }
        
        @Override
        public void update( TriangulationContext tcx )
        {
            DelaunayTriangle t,t2;
            t = tcx.getDebugContext().getPrimaryTriangle();
            t2 = tcx.getDebugContext().getSecondaryTriangle();
            m_node.detachAllChildren();
            if( t != null )
            {
                FloatBuffer fb = m_a.getMeshData().getVertexBuffer();
                fb.rewind();
                fb.put( t.points[0].getXf() ).put( t.points[0].getYf() ).put( t.points[0].getZf() );
                fb.put( t.points[1].getXf() ).put( t.points[1].getYf() ).put( t.points[1].getZf() );
                fb.put( t.points[2].getXf() ).put( t.points[2].getYf() ).put( t.points[2].getZf() );                
                m_node.attachChild( m_a );
            }
            if( t2 != null )
            {
                FloatBuffer fb = m_b.getMeshData().getVertexBuffer();
                fb.rewind();
                fb.put( t2.points[0].getXf() ).put( t2.points[0].getYf() ).put( t2.points[0].getZf() );
                fb.put( t2.points[1].getXf() ).put( t2.points[1].getYf() ).put( t2.points[1].getZf() );
                fb.put( t2.points[2].getXf() ).put( t2.points[2].getYf() ).put( t2.points[2].getZf() );                
                m_node.attachChild( m_b );
            }
        }        
    }

    class CDTSweepActiveNode extends SceneElement<TriangulationContext>
    {
        private Triangle m_a = new Triangle();
        private Triangle m_b = new Triangle();
        private Triangle m_c = new Triangle();
        
        public CDTSweepActiveNode()
        {
            super("WorkingNode");
            m_node.setRenderState( new WireframeState() );
            m_a.setDefaultColor( ColorRGBA.DARK_GRAY );
            m_b.setDefaultColor( ColorRGBA.LIGHT_GRAY );
            m_c.setDefaultColor( ColorRGBA.DARK_GRAY );
            setScale( 0.1 );
        }

        public void setScale( double scale )
        {
            m_a.setScale( scale );
            m_b.setScale( scale );
            m_c.setScale( scale );
        }
        
        @Override
        public void update( TriangulationContext tcx )
        {
            AdvancingFrontNode node = tcx.getDebugContext().getActiveNode();
            TriangulationPoint p;
            if( node != null )
            {                
                if( node.getPrevious() != null ) 
                {
                    p = node.getPrevious().getPoint();
                    m_a.setTranslation( p.getXf(), p.getYf(), p.getZf() );
                }
                p = node.getPoint();
                m_b.setTranslation( p.getXf(), p.getYf(), p.getZf() );
                if( node.getNext() != null )
                {
                    p = node.getNext().getPoint();
                    m_c.setTranslation( p.getXf(), p.getYf(), p.getZf() );
                }
                m_node.attachChild( m_a );
                m_node.attachChild( m_b );
                m_node.attachChild( m_c );
            }
            else
            {
                m_node.detachAllChildren();
            }
        }        
    }

    private ExampleModels m_currentModel = ExampleModels.Two;

    public enum ExampleModels
    {
        Two             ("2.dat",1,0,0,true),
        Debug           ("debug.dat",1,0,0,false),
        Debug2          ("debug2.dat",1,0,0,false),
        Bird            ("bird.dat",1,0,0,false),
        Custom          ("funny.dat",1,0,0,false),
        Diamond         ("diamond.dat",1,0,0,false),
        Dude            ("dude.dat",1,-0.1,0,true),
        Nazca_heron     ("nazca_heron.dat",1.3,0,0.35,false),
        Nazca_monkey    ("nazca_monkey.dat",1,0,0,false),
        Star            ("star.dat",1,0,0,false),
        Strange         ("strange.dat",1,0,0,true),
        Tank            ("tank.dat",1.3,0,0,true);
        
        private final static String m_basePath = "org/poly2tri/examples/data/"; 
        private String m_filename;
        private double m_scale;
        private double m_x;
        private double m_y;
        private boolean _invertedYAxis;
        
        ExampleModels( String filename, double scale, double x, double y, boolean invertedY )
        {
            m_filename = filename;
            m_scale = scale;
            m_x = x;
            m_y = y;
            _invertedYAxis = invertedY;
        }
        
        public String getFilename()
        {
            return m_basePath + m_filename;
        }

        public double getScale()
        {
            return m_scale;
        }
        
        public double getX()
        {
            return m_x;
        }
        
        public double getY()
        {
            return m_y;
        }

        public boolean invertedYAxis()
        {
            return _invertedYAxis;
        }
    }

}