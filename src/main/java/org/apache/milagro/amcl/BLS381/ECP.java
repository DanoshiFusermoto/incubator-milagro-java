/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

/* Elliptic Curve Point class */

package org.apache.milagro.amcl.BLS381;

public final class ECP {

	public static final int WEIERSTRASS=0;
	public static final int EDWARDS=1;
	public static final int MONTGOMERY=2;
	public static final int NOT=0;
	public static final int BN=1;
	public static final int BLS=2;
	public static final int D_TYPE=0;
	public static final int M_TYPE=1;
	public static final int POSITIVEX=0;
	public static final int NEGATIVEX=1;

	public static final int CURVETYPE=WEIERSTRASS;
	public static final int CURVE_PAIRING_TYPE=BLS;
	public static final int SEXTIC_TWIST=M_TYPE;
	public static final int SIGN_OF_X=NEGATIVEX;

	public static final int SHA256=32;
	public static final int SHA384=48;
	public static final int SHA512=64;

	public static final int HASH_TYPE=32;
	public static final int AESKEY=16;

	private FP x;
	private FP y;
	private FP z;
//	private boolean INF;

/* Constructor - set to O */
	ECP(boolean init)
	{
		if (init==true)
		{
			//INF=true;
			x=new FP(0);
			y=new FP(1);
			if (CURVETYPE==EDWARDS)
				z=new FP(1);
			else
				z=new FP(0);
		}
		else
		{
			x = FP.createFast();
			y = FP.createFast();
			z = FP.createFast();
		}
	}
	public ECP() {
		//INF=true;
		x=new FP(0);
		y=new FP(1);
		if (CURVETYPE==EDWARDS)
		{
			z=new FP(1);
		}
		else
		{
			z=new FP(0);
		}
	}

    public ECP(ECP e) {
        this.x = new FP(e.x);
        this.y = new FP(e.y);
        this.z = new FP(e.z);
    }

/* test for O point-at-infinity */
	public boolean is_infinity() {
//		if (INF) return true;                            // Edits made
		if (CURVETYPE==EDWARDS)
		{
			return (x.iszilch() && y.equals(z));
		}
		if (CURVETYPE==WEIERSTRASS)
		{
			return (x.iszilch() && z.iszilch());
		}
		if (CURVETYPE==MONTGOMERY)
		{
			return z.iszilch();
		}
		return true;
	}
/* Conditional swap of P and Q dependant on d */
	private void cswap(ECP Q,int d)
	{
		x.cswap(Q.x,d);
		if (CURVETYPE!=MONTGOMERY) y.cswap(Q.y,d);
		z.cswap(Q.z,d);
	//	if (CURVETYPE!=EDWARDS)
	//	{
	//		boolean bd;
	//		if (d==0) bd=false;
	//		else bd=true;
	//		bd=bd&(INF^Q.INF);
	//		INF^=bd;
	//		Q.INF^=bd;
	//	}
	}

/* Conditional move of Q to P dependant on d */
	private void cmove(ECP Q,int d)
	{
		x.cmove(Q.x,d);
		if (CURVETYPE!=MONTGOMERY) y.cmove(Q.y,d);
		z.cmove(Q.z,d);
	//	if (CURVETYPE!=EDWARDS)
	//	{
	//		boolean bd;
	//		if (d==0) bd=false;
	//		else bd=true;
	//		INF^=(INF^Q.INF)&bd;
	//	}
	}

/* return 1 if b==c, no branching */
	private static int teq(int b,int c)
	{
		int x=b^c;
		x-=1;  // if x=0, x now -1
		return ((x>>31)&1);
	}

/* Constant time select from pre-computed table */
	private void select(ECP W[],int b)
	{
		ECP MP; 
		int m=b>>31;
		int babs=(b^m)-m;

		babs=(babs-1)/2;
		cmove(W[0],teq(babs,0));  // conditional move
		cmove(W[1],teq(babs,1));
		cmove(W[2],teq(babs,2));
		cmove(W[3],teq(babs,3));
		cmove(W[4],teq(babs,4));
		cmove(W[5],teq(babs,5));
		cmove(W[6],teq(babs,6));
		cmove(W[7],teq(babs,7));
 
		MP=new ECP(this);
		MP.neg();
		cmove(MP,(int)(m&1));
	}

/* Test P == Q */
	public boolean equals(ECP Q) {
//		if (is_infinity() && Q.is_infinity()) return true;
//		if (is_infinity() || Q.is_infinity()) return false;

		FP a=new FP(x);                                        // Edits made
		FP b=new FP(Q.x);
		a.mul(Q.z); 
		b.mul(z); 
		if (!a.equals(b)) return false;
		if (CURVETYPE!=MONTGOMERY)
		{
			a.copy(y); a.mul(Q.z); 
			b.copy(Q.y); b.mul(z); 
			if (!a.equals(b)) return false;
		}
		return true;
	}

/* this=P */
	public void copy(ECP P)
	{
		x.copy(P.x);
		if (CURVETYPE!=MONTGOMERY) y.copy(P.y);
		z.copy(P.z);
		//INF=P.INF;
	}
/* this=-this */
	public void neg() {
//		if (is_infinity()) return;
		if (CURVETYPE==WEIERSTRASS)
		{
			y.neg(); y.norm();
		}
		if (CURVETYPE==EDWARDS)
		{
			x.neg(); x.norm();
		}
		return;
	}
/* set this=O */
	public void inf() {
//		INF=true;
		x.zero();
		if (CURVETYPE!=MONTGOMERY) y.one();
		if (CURVETYPE!=EDWARDS) z.zero();
		else z.one();
	}

/* Calculate RHS of curve equation */
	public static FP RHS(FP x) {
		x.norm();
		FP r=new FP(x);
		r.sqr();

		if (CURVETYPE==WEIERSTRASS)
		{ // x^3+Ax+B
			FP b=new FP(new BIG(ROM.CURVE_B));
			r.mul(x);
			if (ROM.CURVE_A==-3)
			{
				FP cx=new FP(x);
				cx.imul(3);
				cx.neg(); cx.norm();
				r.add(cx);
			}
			r.add(b);
		}
		if (CURVETYPE==EDWARDS)
		{ // (Ax^2-1)/(Bx^2-1) 
			FP b=new FP(new BIG(ROM.CURVE_B));

			FP one=new FP(1);
			b.mul(r);
			b.sub(one);
			b.norm();
			if (ROM.CURVE_A==-1) r.neg();
			r.sub(one); r.norm();
			b.inverse();

			r.mul(b);
		}
		if (CURVETYPE==MONTGOMERY)
		{ // x^3+Ax^2+x
			FP x3=new FP(0);
			x3.copy(r);
			x3.mul(x);
			r.imul(ROM.CURVE_A);
			r.add(x3);
			r.add(x);
		}
		r.reduce();
		return r;
	}

/* set (x,y) from two BIGs */
	public ECP(BIG ix,BIG iy) {
		x=new FP(ix);
		y=new FP(iy);
		z=new FP(1);
		FP rhs=RHS(x);

		if (CURVETYPE==MONTGOMERY)
		{
			if (rhs.jacobi()!=1) inf();
			//if (rhs.jacobi()==1) INF=false;
			//else inf();
		}
		else
		{
			FP y2=new FP(y);
			y2.sqr();
			if (!y2.equals(rhs)) inf();
			//if (y2.equals(rhs)) INF=false;
			//else inf();
		}
	}
/* set (x,y) from BIG and a bit */
	public ECP(BIG ix,int s) {
		x=new FP(ix);
		FP rhs=RHS(x);
		y=new FP(0);
		z=new FP(1);
		if (rhs.jacobi()==1)
		{
			FP ny=rhs.sqrt();
			if (ny.redc().parity()!=s) ny.neg();
			y=new FP(ny);
			//INF=false;
		}
		else
		{
			y=new FP(0);
			inf();
		}
	}

/* set from x - calculate y from curve equation */
	public ECP(BIG ix) {
		x=new FP(ix);
		FP rhs=RHS(x);
		z=new FP(1);
		if (rhs.jacobi()==1)
		{
			if (CURVETYPE!=MONTGOMERY) 
				y=new FP(rhs.sqrt());
			//INF=false;
		}
		else 
		{
			y=new FP(0);
			inf(); //INF=true;
		}
	}

/* set to affine - from (x,y,z) to (x,y) */
	public void affine() {
		if (is_infinity()) return;	// 
		if (z.equals(FP.ONE)) return;
		z.inverse();
		x.mul(z); x.reduce();
		if (CURVETYPE!=MONTGOMERY)            // Edits made
		{
			y.mul(z); y.reduce();
		}
		z.copy(FP.ONE);
	}
/* extract x as a BIG */
	public BIG getX()
	{
		ECP W=new ECP(this);
		W.affine();
		return W.x.redc();
	}
/* extract y as a BIG */
	public BIG getY()
	{
		ECP W=new ECP(this);
		W.affine();
		return W.y.redc();
	}

/* get sign of Y */
	public int getS()
	{
		//affine();
		BIG y=getY();
		return y.parity();
	}
/* extract x as an FP */
	public FP getx()
	{
		return x;
	}
/* extract y as an FP */
	public FP gety()
	{
		return y;
	}
/* extract z as an FP */
	public FP getz()
	{
		return z;
	}
/* convert to byte array */
	public void toBytes(byte[] b,boolean compress)
	{
		byte[] t=new byte[BIG.MODBYTES];
		ECP W=new ECP(this);
		W.affine();

		W.x.redc().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) b[i+1]=t[i];

		if (CURVETYPE==MONTGOMERY)
		{
			b[0]=0x06;
			return;
		}

		if (compress)
		{
			b[0]=0x02;
			if (y.redc().parity()==1) b[0]=0x03;
			return;
		}

		b[0]=0x04;

		W.y.redc().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) b[i+BIG.MODBYTES+1]=t[i];
	}
/* convert from byte array to point */
	public static ECP fromBytes(byte[] b)
	{
		byte[] t=new byte[BIG.MODBYTES];
		BIG p=new BIG(ROM.Modulus);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=b[i+1];
		BIG px=BIG.fromBytes(t);
		if (BIG.comp(px,p)>=0) return new ECP();

		if (CURVETYPE==MONTGOMERY)
		{
			return new ECP(px);
		}

		if (b[0]==0x04)
		{
			for (int i=0;i<BIG.MODBYTES;i++) t[i]=b[i+BIG.MODBYTES+1];
			BIG py=BIG.fromBytes(t);
			if (BIG.comp(py,p)>=0) return new ECP();
			return new ECP(px,py);
		}

		if (b[0]==0x02 || b[0]==0x03)
		{
			return new ECP(px,(int)(b[0]&1));
		}
		return new ECP();
	}
/* convert to hex string */
	public String toString() {
		ECP W=new ECP(this);	
		W.affine();
		if (W.is_infinity()) return "infinity";
		if (CURVETYPE==MONTGOMERY) return "("+W.x.redc().toString()+")";
		else return "("+W.x.redc().toString()+","+W.y.redc().toString()+")";
	}

/* convert to hex string */
	public String toRawString() {
		//if (is_infinity()) return "infinity";
		//affine();
		ECP W=new ECP(this);	
		if (CURVETYPE==MONTGOMERY) return "("+W.x.redc().toString()+","+W.z.redc().toString()+")";
		else return "("+W.x.redc().toString()+","+W.y.redc().toString()+","+W.z.redc().toString()+")";
	}

/* this*=2 */
	public void dbl() {
//		if (INF) return;
		
		if (CURVETYPE==WEIERSTRASS)
		{
			if (ROM.CURVE_A==0)
			{
//System.out.println("Into dbl");
				FP t0=new FP(y);                      /*** Change ***/    // Edits made
				t0.sqr();
				FP t1=new FP(y);
				t1.mul(z);
				FP t2=new FP(z);
				t2.sqr();

				z.copy(t0);
				z.add(t0); z.norm(); 
				z.add(z); z.add(z); z.norm();
				t2.imul(3*ROM.CURVE_B_I);

				FP x3=new FP(t2);
				x3.mul(z);

				FP y3=new FP(t0);
				y3.add(t2); y3.norm();
				z.mul(t1); 
				t1.copy(t2); t1.add(t2); t2.add(t1);
				t0.sub(t2); t0.norm(); y3.mul(t0); y3.add(x3);
				t1.copy(x); t1.mul(y); 
				x.copy(t0); x.norm(); x.mul(t1); x.add(x);
				x.norm(); 
				y.copy(y3); y.norm();
//System.out.println("Out of dbl");
			}
			else
			{
				FP t0=new FP(x);
				FP t1=new FP(y);
				FP t2=new FP(z);
				FP t3=new FP(x);
				FP z3=new FP(z);
				FP y3=new FP(0);
				FP x3=new FP(0);
				FP b=new FP(0);

				if (ROM.CURVE_B_I==0)
					b.copy(new FP(new BIG(ROM.CURVE_B)));

				t0.sqr();  //1    x^2
				t1.sqr();  //2    y^2
				t2.sqr();  //3

				t3.mul(y); //4
				t3.add(t3); t3.norm();//5
				z3.mul(x);   //6
				z3.add(z3);  z3.norm();//7
				y3.copy(t2); 
				
				if (ROM.CURVE_B_I==0)
					y3.mul(b); //8
				else
					y3.imul(ROM.CURVE_B_I);
				
				y3.sub(z3); //y3.norm(); //9  ***
				x3.copy(y3); x3.add(y3); x3.norm();//10

				y3.add(x3); //y3.norm();//11
				x3.copy(t1); x3.sub(y3); x3.norm();//12
				y3.add(t1); y3.norm();//13
				y3.mul(x3); //14
				x3.mul(t3); //15
				t3.copy(t2); t3.add(t2); //t3.norm(); //16
				t2.add(t3); //t2.norm(); //17

				if (ROM.CURVE_B_I==0)
					z3.mul(b); //18
				else
					z3.imul(ROM.CURVE_B_I);

				z3.sub(t2); //z3.norm();//19
				z3.sub(t0); z3.norm();//20  ***
				t3.copy(z3); t3.add(z3); //t3.norm();//21

				z3.add(t3); z3.norm(); //22
				t3.copy(t0); t3.add(t0); //t3.norm(); //23
				t0.add(t3); //t0.norm();//24
				t0.sub(t2); t0.norm();//25

				t0.mul(z3);//26
				y3.add(t0); //y3.norm();//27
				t0.copy(y); t0.mul(z);//28
				t0.add(t0); t0.norm(); //29
				z3.mul(t0);//30
				x3.sub(z3); //x3.norm();//31
				t0.add(t0); t0.norm();//32
				t1.add(t1); t1.norm();//33
				z3.copy(t0); z3.mul(t1);//34

				x.copy(x3); x.norm(); 
				y.copy(y3); y.norm();
				z.copy(z3); z.norm();
			}
		}
		if (CURVETYPE==EDWARDS)
		{
//System.out.println("Into dbl");
			FP C=new FP(x);
			FP D=new FP(y);
			FP H=new FP(z);
			FP J=new FP(0);

			x.mul(y); x.add(x); x.norm();
			C.sqr();
			D.sqr();

			if (ROM.CURVE_A==-1) C.neg();	

			y.copy(C); y.add(D); y.norm();
			H.sqr(); H.add(H);

			z.copy(y);
			J.copy(y); 

			J.sub(H); J.norm();
			x.mul(J);

			C.sub(D); C.norm();
			y.mul(C);
			z.mul(J);
//System.out.println("Out of dbl");
		}
		if (CURVETYPE==MONTGOMERY)
		{
			FP A=new FP(x);
			FP B=new FP(x);		
			FP AA=new FP(0);
			FP BB=new FP(0);
			FP C=new FP(0);

			A.add(z); A.norm();
			AA.copy(A); AA.sqr();
			B.sub(z); B.norm();
			BB.copy(B); BB.sqr();
			C.copy(AA); C.sub(BB); C.norm();
			x.copy(AA); x.mul(BB);

			A.copy(C); A.imul((ROM.CURVE_A+2)/4);

			BB.add(A); BB.norm();
			z.copy(BB); z.mul(C);
		}
		return;
	}

/* this+=Q */
	public void add(ECP Q) {
//		if (INF)
//		{
//			copy(Q);
//			return;
//		}
//		if (Q.INF) return;

		if (CURVETYPE==WEIERSTRASS)
		{


			if (ROM.CURVE_A==0)
			{
// Edits made
//System.out.println("Into add");
				int b=3*ROM.CURVE_B_I;
				FP t0=new FP(x);
				t0.mul(Q.x);
				FP t1=new FP(y);
				t1.mul(Q.y);
				FP t2=new FP(z);
				t2.mul(Q.z);
				FP t3=new FP(x);
				t3.add(y); t3.norm();
				FP t4=new FP(Q.x);
				t4.add(Q.y); t4.norm();
				t3.mul(t4);
				t4.copy(t0); t4.add(t1);

				t3.sub(t4); t3.norm();
				t4.copy(y);
				t4.add(z); t4.norm();
				FP x3=new FP(Q.y);
				x3.add(Q.z); x3.norm();

				t4.mul(x3);
				x3.copy(t1);
				x3.add(t2);
	
				t4.sub(x3); t4.norm();
				x3.copy(x); x3.add(z); x3.norm();
				FP y3=new FP(Q.x);
				y3.add(Q.z); y3.norm();
				x3.mul(y3);
				y3.copy(t0);
				y3.add(t2);
				y3.rsub(x3); y3.norm();
				x3.copy(t0); x3.add(t0); 
				t0.add(x3); t0.norm();
				t2.imul(b);

				FP z3=new FP(t1); z3.add(t2); z3.norm();
				t1.sub(t2); t1.norm(); 
				y3.imul(b);
	
				x3.copy(y3); x3.mul(t4); t2.copy(t3); t2.mul(t1); x3.rsub(t2);
				y3.mul(t0); t1.mul(z3); y3.add(t1);
				t0.mul(t3); z3.mul(t4); z3.add(t0);

				x.copy(x3); x.norm(); 
				y.copy(y3); y.norm();
				z.copy(z3); z.norm();
//System.out.println("Out of add");
			}
			else
			{
				FP t0=new FP(x);
				FP t1=new FP(y);
				FP t2=new FP(z);
				FP t3=new FP(x);
				FP t4=new FP(Q.x);
				FP z3=new FP(0);
				FP y3=new FP(Q.x);
				FP x3=new FP(Q.y);
				FP b=new FP(0);

				if (ROM.CURVE_B_I==0)
					b.copy(new FP(new BIG(ROM.CURVE_B)));

				t0.mul(Q.x); //1
				t1.mul(Q.y); //2
				t2.mul(Q.z); //3

				t3.add(y); t3.norm(); //4
				t4.add(Q.y); t4.norm();//5
				t3.mul(t4);//6
				t4.copy(t0); t4.add(t1); //t4.norm(); //7
				t3.sub(t4); t3.norm(); //8
				t4.copy(y); t4.add(z); t4.norm();//9
				x3.add(Q.z); x3.norm();//10
				t4.mul(x3); //11
				x3.copy(t1); x3.add(t2); //x3.norm();//12

				t4.sub(x3); t4.norm();//13
				x3.copy(x); x3.add(z); x3.norm(); //14
				y3.add(Q.z); y3.norm();//15

				x3.mul(y3); //16
				y3.copy(t0); y3.add(t2); //y3.norm();//17

				y3.rsub(x3); y3.norm(); //18
				z3.copy(t2); 
				

				if (ROM.CURVE_B_I==0)
					z3.mul(b); //18
				else
					z3.imul(ROM.CURVE_B_I);
				
				x3.copy(y3); x3.sub(z3); x3.norm(); //20
				z3.copy(x3); z3.add(x3); //z3.norm(); //21

				x3.add(z3); //x3.norm(); //22
				z3.copy(t1); z3.sub(x3); z3.norm(); //23
				x3.add(t1); x3.norm(); //24

				if (ROM.CURVE_B_I==0)
					y3.mul(b); //18
				else
					y3.imul(ROM.CURVE_B_I);

				t1.copy(t2); t1.add(t2); //t1.norm();//26
				t2.add(t1); //t2.norm();//27

				y3.sub(t2); //y3.norm(); //28

				y3.sub(t0); y3.norm(); //29
				t1.copy(y3); t1.add(y3); //t1.norm();//30
				y3.add(t1); y3.norm(); //31

				t1.copy(t0); t1.add(t0); //t1.norm(); //32
				t0.add(t1); //t0.norm();//33
				t0.sub(t2); t0.norm();//34
				t1.copy(t4); t1.mul(y3);//35
				t2.copy(t0); t2.mul(y3);//36
				y3.copy(x3); y3.mul(z3);//37
				y3.add(t2); //y3.norm();//38
				x3.mul(t3);//39
				x3.sub(t1);//40
				z3.mul(t4);//41
				t1.copy(t3); t1.mul(t0);//42
				z3.add(t1); 
				x.copy(x3); x.norm(); 
				y.copy(y3); y.norm();
				z.copy(z3); z.norm();
			}
		}
		if (CURVETYPE==EDWARDS)
		{
//System.out.println("Into add");
			FP A=new FP(z);
			FP B=new FP(0);
			FP C=new FP(x);
			FP D=new FP(y);
			FP E=new FP(0);
			FP F=new FP(0);
			FP G=new FP(0);

			A.mul(Q.z);   
			B.copy(A); B.sqr();    
			C.mul(Q.x);      
			D.mul(Q.y); 

			E.copy(C); E.mul(D);  
		
			if (ROM.CURVE_B_I==0)
			{
				FP b=new FP(new BIG(ROM.CURVE_B));
				E.mul(b);
			}
			else
				E.imul(ROM.CURVE_B_I); 

			F.copy(B); F.sub(E);      
			G.copy(B); G.add(E);       

			if (ROM.CURVE_A==1)
			{
				E.copy(D); E.sub(C);
			}
			C.add(D); 

			B.copy(x); B.add(y);    
			D.copy(Q.x); D.add(Q.y); B.norm(); D.norm(); 
			B.mul(D);                   
			B.sub(C); B.norm(); F.norm(); 
			B.mul(F);                     
			x.copy(A); x.mul(B); G.norm();  
			if (ROM.CURVE_A==1)
			{
				E.norm(); C.copy(E); C.mul(G);  
			}
			if (ROM.CURVE_A==-1)
			{
				C.norm(); C.mul(G);
			}
			y.copy(A); y.mul(C);     

			z.copy(F);	
			z.mul(G);
//System.out.println("Out of add");
		}
		return;
	}

/* Differential Add for Montgomery curves. this+=Q where W is this-Q and is affine. */
	public void dadd(ECP Q,ECP W) {
		FP A=new FP(x);
		FP B=new FP(x);
		FP C=new FP(Q.x);
		FP D=new FP(Q.x);
		FP DA;
		FP CB;	
			
		A.add(z); 
		B.sub(z); 

		C.add(Q.z);
		D.sub(Q.z);
		A.norm();

		D.norm();
		DA=new FP(D); DA.mul(A);

		C.norm();
		B.norm();
		CB=new FP(C); CB.mul(B);

		A.copy(DA); A.add(CB); 
		A.norm(); A.sqr();
		B.copy(DA); B.sub(CB); 
		B.norm(); B.sqr();

		x.copy(A);
		z.copy(W.x); z.mul(B);
	}
/* this-=Q */
	public void sub(ECP Q) {
		ECP NQ=new ECP(Q);
		NQ.neg();
		add(NQ);
	}

/* constant time multiply by small integer of length bts - use ladder */
	public ECP pinmul(int e,int bts) {	
		if (CURVETYPE==MONTGOMERY)
			return this.mul(new BIG(e));
		else
		{
			int nb,i,b;
			ECP P=new ECP(false);
			ECP R0=new ECP(false);
			ECP R1=new ECP(false); R1.copy(this);

			for (i=bts-1;i>=0;i--)
			{
				b=(e>>i)&1;
				P.copy(R1);
				P.add(R0);
				R0.cswap(R1,b);
				R1.copy(P);
				R0.dbl();
				R0.cswap(R1,b);
			}
			P.copy(R0);
			P.affine();
			return P;
		}
	}

/* return e.this */

	public ECP mul(BIG e) {
		if (e.iszilch() || is_infinity()) return new ECP();
		ECP P;
		if (CURVETYPE==MONTGOMERY)
		{
/* use Ladder */
			int nb,i,b;
			P=new ECP(false);
			ECP D=new ECP();
			ECP R0=new ECP(); R0.copy(this);
			ECP R1=new ECP(); R1.copy(this);
			R1.dbl();

			D.copy(this); D.affine();
			nb=e.nbits();
			for (i=nb-2;i>=0;i--)
			{
				b=e.bit(i);
				P.copy(R1);

				P.dadd(R0,D);
				R0.cswap(R1,b);
				R1.copy(P);
				R0.dbl();
				R0.cswap(R1,b);

			}

			P.copy(R0);
		}
		else
		{
// fixed size windows 
			int i,b,nb,m,s,ns;
			BIG mt=BIG.createFast();
			BIG t=BIG.createFast();
			ECP Q;
			ECP C;
			ECP[] W=new ECP[8];
			byte[] w=new byte[1+(BIG.NLEN*BIG.BASEBITS+3)/4];

			//affine();

// precompute table 
			Q=new ECP(this);

			Q.dbl();
			W[0]=new ECP(this);

			for (i=1;i<8;i++)
			{
				W[i]=new ECP(W[i-1]);
				W[i].add(Q);
			}

// make exponent odd - add 2P if even, P if odd 
			t.copy(e);
			s=t.parity();
			t.inc(1); t.norm(); ns=t.parity(); mt.copy(t); mt.inc(1); mt.norm();
			t.cmove(mt,s);
			Q.cmove(this,ns);
			C=new ECP(Q);

			nb=1+(t.nbits()+3)/4;

// convert exponent to signed 4-bit window 
			for (i=0;i<nb;i++)
			{
				w[i]=(byte)(t.lastbits(5)-16);
				t.dec(w[i]); t.norm();
				t.fshr(4);	
			}
			w[nb]=(byte)t.lastbits(5);
	
			P=new ECP(W[(w[nb]-1)/2]);  
			for (i=nb-1;i>=0;i--)
			{
				Q.select(W,w[i]);
				P.dbl();
				P.dbl();
				P.dbl();
				P.dbl();
				P.add(Q);
			}
			P.sub(C); /* apply correction */
		}
		P.affine();
		return P;
	}

/* Return e.this+f.Q */

	public ECP mul2(BIG e,ECP Q,BIG f) {
		BIG te;
		BIG tf;
		BIG mt;
		ECP S;
		ECP T;
		ECP C;
		ECP[] W=new ECP[8];
		byte[] w=new byte[1+(BIG.NLEN*BIG.BASEBITS+1)/2];		
		int i,s,ns,nb;
		byte a,b;

		//affine();
		//Q.affine();

		te=new BIG(e);
		tf=new BIG(f);

// precompute table 
		W[1]=new ECP(this); W[1].sub(Q);
		W[2]=new ECP(this); W[2].add(Q);
		S=new ECP(Q); S.dbl();
		W[0]=new ECP(W[1]); W[0].sub(S);
		W[3]=new ECP(W[2]); W[3].add(S);
		T=new ECP(this); T.dbl();
		W[5]=new ECP(W[1]); W[5].add(T);
		W[6]=new ECP(W[2]); W[6].add(T);
		W[4]=new ECP(W[5]); W[4].sub(S);
		W[7]=new ECP(W[6]); W[7].add(S);

// if multiplier is odd, add 2, else add 1 to multiplier, and add 2P or P to correction 

		s=te.parity();
		te.inc(1); te.norm(); ns=te.parity(); 
		mt=new BIG(te); mt.inc(1); mt.norm();
		te.cmove(mt,s);
		T.cmove(this,ns);
		C=new ECP(T);

		s=tf.parity();
		tf.inc(1); tf.norm(); ns=tf.parity(); 
		mt.copy(tf); mt.inc(1); mt.norm();
		tf.cmove(mt,s);
		S.cmove(Q,ns);
		C.add(S);

		mt.copy(te); mt.add(tf); mt.norm();
		nb=1+(mt.nbits()+1)/2;

// convert exponent to signed 2-bit window 
		for (i=0;i<nb;i++)
		{
			a=(byte)(te.lastbits(3)-4);
			te.dec(a); te.norm(); 
			te.fshr(2);
			b=(byte)(tf.lastbits(3)-4);
			tf.dec(b); tf.norm(); 
			tf.fshr(2);
			w[i]=(byte)(4*a+b);
		}
		w[nb]=(byte)(4*te.lastbits(3)+tf.lastbits(3));
		S.copy(W[(w[nb]-1)/2]);  

		for (i=nb-1;i>=0;i--)
		{
			T.select(W,w[i]);
			S.dbl();
			S.dbl();
			S.add(T);
		}
		S.sub(C); /* apply correction */
		S.affine();
		return S;
	}

// multiply a point by the curves cofactor
	public void cfp()
	{
		int cf=ROM.CURVE_Cof_I;
		if (cf==1) return;
		if (cf==4)
		{
			dbl(); dbl();
			//affine();
			return;
		} 
		if (cf==8)
		{
			dbl(); dbl(); dbl();
			//affine();
			return;
		}
		BIG c=new BIG(ROM.CURVE_Cof);
		copy(mul(c));
	}

/* Map byte string to curve point */
	public static ECP mapit(byte[] h)
	{
		BIG q=BIG.BIGROMMOD;
		BIG x=BIG.fromBytes(h);
		x.mod(q);
		ECP P;

		while (true)
		{
			while (true)
			{
				if (CURVETYPE!=MONTGOMERY)
					P=new ECP(x,0);
				else
					P=new ECP(x);	
				x.inc(1); x.norm();
				if (!P.is_infinity()) break;
			}
			P.cfp();
			if (!P.is_infinity()) break;
		}
		return P;
	}

	public static ECP generator()
	{
		ECP G;
		BIG gx,gy;
		gx=new BIG(ROM.CURVE_Gx);

		if (ECP.CURVETYPE!=ECP.MONTGOMERY)
		{
			gy=new BIG(ROM.CURVE_Gy);
			G=new ECP(gx,gy);
		}
		else
			G=new ECP(gx);
		return G;
	}

/*
	public static void main(String[] args) {

		BIG Gx=new BIG(ROM.CURVE_Gx);
		BIG Gy;
		ECP P;
		if (CURVETYPE!=MONTGOMERY) Gy=new BIG(ROM.CURVE_Gy);
		BIG r=new BIG(ROM.CURVE_Order);

		//r.dec(7);
	
		System.out.println("Gx= "+Gx.toString());		
		if (CURVETYPE!=MONTGOMERY) System.out.println("Gy= "+Gy.toString());	

		if (CURVETYPE!=MONTGOMERY) P=new ECP(Gx,Gy);
		else  P=new ECP(Gx);

		System.out.println("P= "+P.toString());		

		ECP R=P.mul(r);
		//for (int i=0;i<10000;i++)
		//	R=P.mul(r);
	
		System.out.println("R= "+R.toString());
    } */
}

