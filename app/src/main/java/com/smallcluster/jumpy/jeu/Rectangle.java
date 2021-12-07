package com.smallcluster.jumpy.jeu;

public class Rectangle {
    protected float x,y,width,height;

    public Rectangle(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean collision(Rectangle autre){
        return !(autre.getX()-autre.getWidth()/2 > x+width/2 || autre.getX()+autre.getWidth()/2 < x-width/2 ||
                autre.getY()-autre.getHeight()/2 > y+height/2 || autre.getY()+ autre.getHeight()/2 < y-height/2);
    }
}
