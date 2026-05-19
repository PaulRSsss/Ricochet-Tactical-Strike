package com.mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class BulletControl extends AbstractControl {

    private int screenWidth, screenHeight;
    private float speed = 800f; 
    public Vector3f direction;
    private float rotation;
    
    private int bounces = 0;
    private final int MAX_BOUNCES = 10; 
    
    private Node enemyNode;
    private Node obstacleNode; // NUEVO
    private Main mainApp;

    // Actualizamos el constructor para recibir el obstacleNode
    public BulletControl(Vector3f direction, int screenWidth, int screenHeight, Node enemyNode, Node obstacleNode, Main mainApp) {
        this.direction = direction;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.enemyNode = enemyNode;
        this.obstacleNode = obstacleNode;
        this.mainApp = mainApp;
    }

    @Override
    protected void controlUpdate(float tpf) {
        spatial.move(direction.mult(speed * tpf));

        float actualRotation = Main.getAngleFromVector(direction);
        if (actualRotation != rotation) {
            spatial.rotate(0, 0, actualRotation - rotation);
            rotation = actualRotation;
        }

        Vector3f loc = spatial.getLocalTranslation();
        boolean bouncedThisFrame = false;

        // Rebotes en bordes de la pantalla
        if (loc.x > screenWidth || loc.x < 0) {
            direction.x *= -1; 
            bouncedThisFrame = true;
            spatial.setLocalTranslation(loc.x < 0 ? 1 : screenWidth - 1, loc.y, loc.z);
        }
        
        if (loc.y > screenHeight || loc.y < 0) {
            direction.y *= -1; 
            bouncedThisFrame = true;
            spatial.setLocalTranslation(loc.x, loc.y < 0 ? 1 : screenHeight - 1, loc.z);
        }

        // Rebotes en las plataformas (Cálculo AABB)
        for (Spatial plat : obstacleNode.getChildren()) {
            // Obtenemos las medidas reales multiplicadas por la escala
            float platWidth = (Float) plat.getUserData("width") * plat.getLocalScale().x;
            float platHeight = (Float) plat.getUserData("height") * plat.getLocalScale().y;
            Vector3f pLoc = plat.getLocalTranslation();
            
            // Verificamos si la posición de la bala entró en el rectángulo de la plataforma
            if (loc.x > pLoc.x - platWidth / 2 && loc.x < pLoc.x + platWidth / 2 &&
                loc.y > pLoc.y - platHeight / 2 && loc.y < pLoc.y + platHeight / 2) {
                
                // Determinamos si chocó por el lado X o por el lado Y para invertir el vector
                float diffX = loc.x - pLoc.x;
                float diffY = loc.y - pLoc.y;
                
                if (Math.abs(diffX) / platWidth > Math.abs(diffY) / platHeight) {
                    direction.x *= -1; // Chocó por un lado (izquierdo o derecho)
                } else {
                    direction.y *= -1; // Chocó por arriba o por abajo
                }
                
                bouncedThisFrame = true;
                // Movemos un poco la bala hacia atrás para evitar que se quede atorada adentro de la plataforma
                spatial.move(direction.mult(speed * tpf)); 
            }
        }

        if (bouncedThisFrame) {
            bounces++;
            if (bounces > MAX_BOUNCES) {
                spatial.removeFromParent(); 
                return; 
            }
        }

        // Colisión con enemigos
        for (Spatial enemy : enemyNode.getChildren()) {
            float dist = enemy.getLocalTranslation().distance(loc);
            // Multiplicamos el radio por la escala para que el hitbox sea exacto a la imagen
            float radius = (Float) enemy.getUserData("radius") * enemy.getLocalScale().x;
            
            if (dist < radius) {
                enemy.removeFromParent(); 
                mainApp.enemiesAlive--; 
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}