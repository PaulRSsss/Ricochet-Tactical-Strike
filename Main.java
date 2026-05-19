package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

public class Main extends SimpleApplication implements AnalogListener {

    private Spatial player;
    private Node bulletNode;
    private Node enemyNode;
    private Node obstacleNode;
    
    private int bulletsFired = 0;
    private final int MAX_BULLETS = 3;
    public int enemiesAlive = 6;
    private long bulletCooldown = 0;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0, 0, 0.2f));
        getFlyByCamera().setEnabled(false);
        setDisplayStatView(false);
        setDisplayFps(false);

        bulletNode = new Node("bullets");
        enemyNode = new Node("enemies");
        obstacleNode = new Node("obstacles"); 
        
        guiNode.attachChild(bulletNode);
        guiNode.attachChild(enemyNode);
        guiNode.attachChild(obstacleNode); // Lo metemos a la pantalla

        // JUGADOR: Lo hacemos a la mitad de su tamaño original con setLocalScale(0.5f)
        player = getSpatial("Player"); 
        player.setLocalScale(0.2f); // <-- AQUÍ MANEJAS LA MEDIDA
        player.setLocalTranslation(settings.getWidth() / 2, 50, 0); 
        guiNode.attachChild(player);

        // PLATAFORMAS (Obstáculos)
        // Coordenadas X, Y, y qué tan anchas o altas las quieres (escala X, escala Y)
        spawnPlatform(300, 400, 0.6f, 0.2f); // Plataforma horizontal en el centro
        spawnPlatform(200, 200, 0.5f, 0.2f); // Muro vertical a la izquierda
        spawnPlatform(600, 250, 0.5f, 0.2f); // Muro vertical a la derecha

        // ENEMIGOS:
        spawnEnemy(350, 350, 0.5f);
        spawnEnemy(250, 300, 0.5f); 
        spawnEnemy(650, 250, 0.5f);
        spawnEnemy(100, 200, 0.5f);
        spawnEnemy(700, 100, 0.5f);
        spawnEnemy(350, 150, 0.5f);

        inputManager.addMapping("mousePick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "mousePick");
    }

    private Spatial getSpatial(String name) {
        Node node = new Node(name);
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/" + name + ".png");
        pic.setTexture(assetManager, tex, true);

        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width / 2f, -height / 2f, 0);

        Material picMat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        node.setMaterial(picMat);

        // Guardamos las medidas reales del objeto para calcular los choques
        node.setUserData("radius", width / 2);
        node.setUserData("width", width);
        node.setUserData("height", height);
        node.attachChild(pic);
        
        return node;
    }

    private void spawnEnemy(float x, float y, float scale) {
        Spatial enemy = getSpatial("Enemy"); 
        enemy.setLocalTranslation(x, y, 0);
        enemy.setLocalScale(0.1f); // <-- AQUÍ MANEJAS LA MEDIDA
        enemyNode.attachChild(enemy);
    }
    
    // Método nuevo para crear las plataformas
    private void spawnPlatform(float x, float y, float scaleX, float scaleY) {
        Spatial platform = getSpatial("Platform"); 
        platform.setLocalTranslation(x, y, 7);
        platform.setLocalScale(scaleX, scaleY, 0.01f); // La estiramos como queramos
        obstacleNode.attachChild(platform);
    }

    private Vector3f getAimDirection() {
        Vector2f mouse = inputManager.getCursorPosition();
        Vector3f playerPos = player.getLocalTranslation();
        Vector3f dif = new Vector3f(mouse.x - playerPos.x, mouse.y - playerPos.y, 0);
        return dif.normalizeLocal();
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("mousePick") && bulletsFired < MAX_BULLETS) {
            if (System.currentTimeMillis() - bulletCooldown > 500f) { 
                bulletCooldown = System.currentTimeMillis();
                bulletsFired++;
                
                Vector3f aim = getAimDirection();
                Spatial bullet = getSpatial("Bullet");
                bullet.setLocalScale(0.02f); // Bala un poco más pequeña
                
                Vector3f offset = aim.mult(30);
                bullet.setLocalTranslation(player.getLocalTranslation().add(offset));
                
                // Le pasamos también el obstacleNode a la bala
                bullet.addControl(new BulletControl(aim, settings.getWidth(), settings.getHeight(), enemyNode, obstacleNode, this));
                bulletNode.attachChild(bullet);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (enemiesAlive <= 0) {
            System.out.println("¡GANASTE! Mataste a todos los enemigos.");
        } else if (bulletsFired >= MAX_BULLETS && bulletNode.getQuantity() == 0) {
            System.out.println("FIN DEL JUEGO. Te quedaste sin balas.");
        }
    }
    
    public static float getAngleFromVector(Vector3f vec) {
        Vector2f vec2 = new Vector2f(vec.x, vec.y);
        return vec2.getAngle();
    }
}